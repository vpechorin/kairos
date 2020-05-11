package net.pechorina.kairos.core.verticles.http

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.ext.web.handler.TimeoutHandler
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.Constants
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.http.ContentTypeUtils
import net.pechorina.kairos.core.http.Http
import net.pechorina.kairos.core.http.HttpMethod
import net.pechorina.kairos.core.http.HttpRoute
import net.pechorina.kairos.core.http.HttpTargetBuilder
import net.pechorina.kairos.core.http.handlers.AuthenticationHandler
import net.pechorina.kairos.core.http.handlers.AuthorizationHandler
import net.pechorina.kairos.core.mappers.GroovyObjectMapper
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.utils.JsonMapper
import net.pechorina.kairos.core.utils.TemplateEngine
import net.pechorina.kairos.core.utils.XmlObjectMapper
import net.pechorina.kairos.core.utils.stackTraceString
import net.pechorina.kairos.core.verticles.StageVerticle
import java.net.URLEncoder

class HttpServerSource : StageVerticle() {

    private lateinit var httpServer: HttpServer
    private lateinit var router: Router
    private var port: Int = 6080
    private var routes: List<HttpRoute> = arrayListOf()
    private val groovyObjectMapper: GroovyObjectMapper = GroovyObjectMapper()
    private val mapType = JsonMapper.jsonObjectMapper.typeFactory.constructMapType(
            LinkedHashMap::class.java, String::class.java, Object::class.java
    )

    private val env = System.getenv()
            ?.entries
            ?.filter { it.key.isNullOrBlank().not() && it.value.isNullOrEmpty().not() }
            ?.associateBy({ entry -> entry.key }, { entry -> entry.value })

    override fun start() {
        super.start()

        port = definition.opts.section(ConfigKey.Http.key).block().getInteger(ConfigKey.Port.key, 6080)
        routes = getRoutes()

        val httpServerOptions = HttpServerOptions().setPort(port)
        router = Router.router(vertx)
        httpServer = vertx.createHttpServer(httpServerOptions)
        httpServer.requestHandler(router).listen(port)

        router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT))
        router.route().handler(BodyHandler.create())
        router.route().handler(ResponseTimeHandler.create())

        val vertxConfig = config().getJsonObject("config")

        val authEnabled = definition.opts.section(ConfigKey.Http.key)
                .block().getBool(ConfigKey.AuthEnabled.key, false)

        if (authEnabled) {
            val jwtAuthAddress = definition.opts
                    .section(ConfigKey.AuthN.key)
                    .block().getString(ConfigKey.JWTAuthServiceAddress.key)

            val usernamePasswordAuthAddress = definition.opts
                    .section(ConfigKey.AuthN.key)
                    .block().getString(ConfigKey.UsernamePasswordAuthServiceAddress.key)
            log.debug("Authentication enabled: [AuthServices at JWT:${jwtAuthAddress}, Username/Pass:${usernamePasswordAuthAddress}]")
            router.route().handler(AuthenticationHandler(usernamePasswordAuthAddress, jwtAuthAddress, vertx))
        }

        routes.forEach { route -> setupRoute(route, authEnabled) }
    }

    private fun setupRoute(routeConfig: HttpRoute, authEnabled: Boolean) {
        log.debug("Setup route[{}][Auth={}]: {}, ", routeConfig.route, authEnabled, routeConfig.toYamlString())
        if (!isValid(routeConfig)) {
            log.warn { "Route validation failed" }
            return
        }

        val route = if (routeConfig.method == null) {
            router.route(routeConfig.route)
        } else {
            val configuredMethod = routeConfig.method
            val method = io.vertx.core.http.HttpMethod.valueOf(configuredMethod!!.name)
            router.route(method, routeConfig.route)
        }

        if (routeConfig.consumes != null) {
            route.consumes(routeConfig.consumes)
        }
        if (routeConfig.timeout != null) {
            route.handler(TimeoutHandler.create(routeConfig.timeout!!))
        }

        // attach authorization handler
        if (authEnabled && routeConfig.authorities != null) {
            route.handler(
                    AuthorizationHandler(
                            routeConfig.authorities!!.split(",".toRegex()).map { it.trim() }.toHashSet()
                    )
            )
        }

        route.handler { routingContext -> handle(routingContext, routeConfig) }
    }

    private fun isValid(routeConfig: HttpRoute): Boolean {
        val outputLaneName: String = routeConfig.output ?: Constants.STDOUT
        var outputLane = getOutputLane(outputLaneName)
        if (outputLane == null) {
            log.error("Config error: Output lane[$outputLaneName] was not found")
            return false
        }
        return true
    }

    private fun handle(routingContext: RoutingContext, routeConfig: HttpRoute) {
        val requestBody = when (routingContext.request().method()) {
            io.vertx.core.http.HttpMethod.POST,
            io.vertx.core.http.HttpMethod.PUT,
            io.vertx.core.http.HttpMethod.PATCH -> routingContext.body
            else -> null
        }

        val contentType = getContentType(routingContext, requestBody)
        log.debug { "route[${routeConfig.method}|${routeConfig.route}] <-- req[${routingContext.request().uri()}|${routingContext.request().getHeader("Content-Type")}]" }

        val acceptContentType = getAcceptMediaType(routingContext.request())
        log.debug { "Headers: ${routingContext.request().headers()}" }

        val outputLaneName: String = routeConfig.output ?: Constants.STDOUT
        var outputLane = getOutputLane(outputLaneName)

        val event: KEvent

        try {
            event = transformInput(routingContext, contentType, requestBody, routeConfig)
        } catch (e: Exception) {
            log.error("Error creating event", e)
            respondWithError(routingContext, 500, stackTraceString(e))
            return
        }


        log.debug { "Event: ${event}" }

        if (outputLane!!.type == IOLaneType.INTERACTIVE) {
            sendEvent(outputLane, event, Handler { replyResult -> reply(routingContext, replyResult, routeConfig) })
            return
        }

        publishOutputEvent(event, outputLane.name)
        log.debug { "Closing response - OK" }
        respondOK(routingContext)
    }

    private fun respondOK(routingContext: RoutingContext, buffer: Buffer? = null, contentType: ContentType = ContentType.JSON) {
        if (buffer == null) {
            routingContext.response().statusCode = 204
            routingContext.response().end()
        } else {
            routingContext.response().statusCode = 200
            routingContext.response().putHeader("Content-Type", contentType.mediaType)
            routingContext.response().end(buffer)
        }
    }

    private fun respondWithError(routingContext: RoutingContext, code: Int, message: String?) {
        routingContext.response().statusCode = code
        routingContext.response().end(message ?: "Error ${code}")
    }

    private fun reply(routingContext: RoutingContext, replyResult: AsyncResult<Message<KEvent>>, routeConfig: HttpRoute) {
        if (replyResult.failed()) {
            log.debug { "Reply with failure: ${replyResult.cause()}" }
            respondWithError(routingContext, 500, replyResult.cause().message)
            return
        }
        val acceptType = getAcceptMediaType(routingContext.request())
        log.debug("Accept: {}", acceptType)
        val message = replyResult.result()
        val event = message.body()
        val buffer: Buffer
        try {
            buffer = toBuffer(event, routeConfig)
        } catch (e: Exception) {
            log.error("Error mapping output with " + routeConfig.responseMapperEx, e)
            respondWithError(routingContext, 500, stackTraceString(e))
            return
        }

        respondOK(routingContext, buffer)
    }

    private fun toBuffer(event: KEvent, routeConfig: HttpRoute): Buffer {
        log.debug { "toBuffer: expression: ${routeConfig.responseMapperEx}, content-type:${event.getContentType()}, body:${event.body.length()} bytes" }
        return if (routeConfig.responseMapperEx.isNullOrEmpty()) {
            log.debug { "Pass the reply body to the response without transformation" }
            event.body
        } else {
            log.debug { "Transform the reply body" }
            Buffer.buffer(
                    JsonMapper.jsonObjectMapper.writeValueAsBytes(
                            groovyObjectMapper.map(routeConfig.responseMapperEx, event)
                    )
            )
        }
    }

    private fun getContentType(routingContext: RoutingContext, body: Buffer?): ContentType {
        val contentTypeString = routingContext.request().getHeader("Content-Type")
        log.debug { "Content-Type header: $contentTypeString" }
        val detectedType = when (contentTypeString) {
            null -> ContentType.TEXT
            else -> ContentTypeUtils.getContentType(contentTypeString)
        }
        if (detectedType == ContentType.JSON) {
            if (body == null) return detectedType
            return if (ContentTypeUtils.isJsonArray(body.toString())) ContentType.JSONA else ContentType.JSON
        }
        return detectedType
    }

    private fun requestParamsToRequestMap(routingContext: RoutingContext): Map<String, String> {
        val map = HashMap<String, String>()

        if (routingContext.pathParams() != null) map.putAll(routingContext.pathParams())
        if (routingContext.queryParams() != null) {
            val multiMap = routingContext.queryParams()
            for (key in multiMap.names()) {
                map[key] = multiMap.getAll(key).joinToString(",")
            }
        }

        return map;
    }

    private fun transformInput(routingContext: RoutingContext, contentType: ContentType, body: Buffer?, routeConfig: HttpRoute): KEvent {

        val requestMap = requestParamsToRequestMap(routingContext)
        val headerMap = getHeaders(routingContext)

        if (routeConfig.requestMapperEx.isNullOrBlank()) {
            val e = if (body != null)
                KEvent(body = body).setContentType(contentType)
                        .addHeaders(headerMap)
                        .addHeaders(requestMap)
                        .addPath(id())
            else
                KEvent(JsonObject(requestMap))
                        .addHeaders(headerMap)
                        .addPath(id())
            addProxyPassHeaders(e, routingContext, routeConfig)
            return e
        }

        val input = if (body == null || body.length() == 0) {
            requestMap
        } else {
            when (contentType) {
                ContentType.JSON -> JsonMapper.jsonObjectMapper.readValue(body.bytes, mapType)
                ContentType.XML -> XmlObjectMapper.xmlObjectMapper.readValue(body.bytes, mapType)
                else -> KEvent.asContentType(body, contentType)
            }
        }

        val mapped = groovyObjectMapper.map(routeConfig.requestMapperEx, input, requestMap)

        val outputContentType = routeConfig.requestMapperOutputType ?: contentType

        val outEvent = when (outputContentType) {
            ContentType.JSON -> KEvent(JsonObject.mapFrom((mapped))).addPath(id()).addHeaders(headerMap)
            ContentType.JSONA -> KEvent(JsonArray(mapped as List<*>)).addPath(id()).addHeaders(headerMap)
            ContentType.XML -> KEvent(XmlObjectMapper.xmlObjectMapper.writeValueAsString(mapped)).setContentType(ContentType.XML).addPath(id()).addHeaders(headerMap)
            else -> KEvent(mapped.toString()).addPath(id()).addHeaders(headerMap)
        }

        addProxyPassHeaders(outEvent, routingContext, routeConfig)

        return outEvent
    }

    private fun addProxyPassHeaders(event: KEvent, routingContext: RoutingContext, routeConfig: HttpRoute) {
        if (routeConfig.proxyPass.isNullOrBlank() && routeConfig.proxyPassEx.isNullOrBlank()) return

        var targetAddress: String? = null
        routeConfig.proxyPass?.let { targetAddress = it }

        routeConfig.proxyPassEx?.let {
            val templateContext = mapOf(
                    "env" to env,
                    "route" to routeConfig,
                    "ctx" to routingContext,
                    "config" to config()
            )

            targetAddress = TemplateEngine.process(it, templateContext)
        }

        if (targetAddress.isNullOrEmpty().not()) {
            val targetURI = HttpTargetBuilder(
                    pathParams = routingContext.pathParams(),
                    currentPath = routingContext.normalisedPath(),
                    queryParams = routingContext.queryParams().groupBy({ e -> e.key }, { e -> e.value }),
                    routePath = routeConfig.route,
                    target = targetAddress!!
            ).build()
            event.addHeader(Http.TARGET_HEADER, targetURI)
        }
    }

    private fun getHeaders(rc: RoutingContext): Map<String, String> {
        val headerMap = linkedMapOf<String, String>()
        if (rc.queryParams() != null && !rc.queryParams().isEmpty) {
            headerMap[Http.QUERY_STRING] = asString(rc.queryParams())
        }
        if (rc.request().absoluteURI() != null) headerMap[Http.URI] = rc.request().absoluteURI()

        headerMap += asMap(rc.queryParams(), Http.QUERY_PARAM_PREFIX)
        headerMap += pathParams(rc.pathParams(), Http.PATH)
        headerMap += asMap(rc.request().headers(), Http.HEADER_PREFIX)
        headerMap += asMap(rc.request(), Http.REQUEST_PREFIX)
        return headerMap
    }

    private fun asMap(request: HttpServerRequest, prefix: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()

        map["${prefix}version"] = request.version().name
        map["${prefix}method"] = request.method().name
        map["${prefix}rawMethod"] = request.rawMethod()
        map["${prefix}isSSL"] = java.lang.Boolean.toString(request.isSSL)
        map["${prefix}method"] = request.method().name
        map["${prefix}scheme"] = request.scheme()
        map["${prefix}uri"] = request.uri()
        map["${prefix}path"] = request.path()
        request.query()?.let { map["${prefix}query"] = it }
        map["${prefix}host"] = request.host()
        map["${prefix}remoteAddress"] = request.remoteAddress().toString()
        map["${prefix}localAddress"] = request.localAddress().toString()

        return map
    }

    private fun asMap(m: MultiMap?, prefix: String): Map<String, String> {
        if (m == null) return emptyMap()
        return m.entries()
                .groupBy { it.key }
                .entries
                .associateBy(
                        { "${prefix}.${it.key}" },
                        { it.value.joinToString(",") }
                )
    }

    private fun asString(m: MultiMap): String {
        val list = mutableListOf<String>()
        for ((key, value) in m) {
            list.add(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8"))
        }
        return list.joinToString("&")
    }

    private fun pathParams(m: Map<String, String>?, prefix: String): Map<String, String> {
        if (m == null) return emptyMap()
        return m.entries.associateBy({ "$prefix.${it.key}" }, { it.value })
    }

    private fun getRoutes(): List<HttpRoute> {
        return definition.opts.section(ConfigKey.Routes.name).blocks
                .map { it.transformTo(HttpRoute::class.java) }
    }

    private fun toHttpMethod(m: HttpMethod?): io.vertx.core.http.HttpMethod {
        return if (m == null) {
            io.vertx.core.http.HttpMethod.GET
        } else {
            io.vertx.core.http.HttpMethod.valueOf(m.name.toUpperCase())
        }
    }

    private fun getAcceptMediaType(request: HttpServerRequest): ContentType? {
        return listOf("accept", "Accept")
                .map { request.getHeader(it) }
                .flatMap { ContentTypeUtils.getAllContentTypes(it) }
                .firstOrNull()
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }

}
