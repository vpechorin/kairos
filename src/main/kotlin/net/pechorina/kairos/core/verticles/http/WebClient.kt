package net.pechorina.kairos.core.verticles.http

import io.netty.handler.codec.http.QueryStringDecoder
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.Block
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.auth.UsernamePassword
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.http.ContentTypeUtils
import net.pechorina.kairos.core.http.Http
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.types.mapStatusCodeToStatus
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle
import java.net.URI
import java.net.URL
import java.net.URLDecoder

class WebClient : StageCoroutineVerticle() {

    lateinit var client: WebClient

    var usernamePassword: UsernamePassword? = null
    var token: String? = null

    override suspend fun start() {
        super.start()
        val webClientOptionsBlock = definition.opts.section(ConfigKey.WebClientOptions.key).block()

        val basicAuthBlock = definition.opts.section(ConfigKey.WebClientBasicAuth.key).block()
        val user = basicAuthBlock.getString(ConfigKey.Username.key)
        val password = basicAuthBlock.getString(ConfigKey.Password.key)
        if (!user.isNullOrBlank() && !password.isNullOrBlank()) usernamePassword = UsernamePassword(user, password)

        val jwtAuthBlock = definition.opts.section(ConfigKey.WebClientJWTAuth.key).block()
        jwtAuthBlock.getString(ConfigKey.Token.key)?.let { token = it }

        val webClientOptions = readOptions(webClientOptionsBlock)
        this.client = WebClient.create(vertx, webClientOptions)

        subscribe(IOLaneType.INTERACTIVE, Handler { this.handle(it, true) })
        subscribe(IOLaneType.EVENT, Handler { this.handle(it, false) })
    }

    private fun readOptions(configBlock: Block): WebClientOptions {
        try {
            return WebClientOptions(
                    JsonObject.mapFrom(configBlock.options)
            )
        } catch (e: Throwable) {
            log.error(e) { "Error reading WebClientConfig" }
        }
        return WebClientOptions()
    }

    private fun handle(message: Message<KEvent>, interactive: Boolean = false) {
        val inEvent = message.body()

        log.debug { "Processing event: ${inEvent}" }

        val target = inEvent.getHeader(Http.TARGET_HEADER) ?: "http://localhost";
        val path = inEvent.getHeader(Http.TARGET_PATH_HEADER) ?: ""
        val targetURI = makeTargetURI(target, path)
        handle(message, targetURI, interactive)
    }

    private fun handle(message: Message<KEvent>, target: URI, interactive: Boolean = false) {
        val inEvent = message.body()
        val method = inEvent.getHeader(Http.METHOD_HEADER) ?: "GET"
        val uriString = inEvent.getHeader(Http.TARGET_HEADER)
                ?: throw IllegalArgumentException("TARGET_HEADER is missing")
        val target = URI(uriString)

        log.debug { "Process ${method} request ${uriString}" }

        val request = when (method) {
            "GET" -> client.get(target.port, target.host, target.path)
            "HEAD" -> client.head(target.port, target.host, target.path)
            "DELETE" -> client.delete(target.port, target.host, target.path)
            "POST" -> client.post(target.port, target.host, target.path)
            "PUT" -> client.put(target.port, target.host, target.path)
            "PATCH" -> client.patch(target.port, target.host, target.path)
            else -> client.get(target.port, target.host, target.path)
        }

        usernamePassword?.let { request.basicAuthentication(it.username, it.password) }
        token?.let { request.bearerTokenAuthentication(it) }

        if (target.query.isNullOrEmpty().not()) {
            val decodedParams = QueryStringDecoder(target).parameters()
            if (decodedParams.isNotEmpty()) {
                for ((key, value) in decodedParams) value.forEach { v -> queryParamSetter(key, v, request) }
            }
        }

        val headers = getHeaders(inEvent);
        if (!headers.isEmpty) {
            request.putHeaders(headers)
            log.debug { "Set request headers: ${headers}" }
        }

        val resultHandler: (AsyncResult<HttpResponse<Buffer>>) -> Unit = { result ->
            if (result.succeeded()) {
                onSuccess(message, result.result(), interactive)
            } else {
                onFailure(message, result.cause(), interactive)
            }
        }

        val body = inEvent.body

        when (method) {
            "GET" -> request.send(resultHandler)
            "HEAD" -> request.send(resultHandler)
            "DELETE" -> request.send(resultHandler)
            "POST" -> request.sendBuffer(body, resultHandler)
            "PUT" -> request.sendBuffer(body, resultHandler)
            "PATCH" -> request.sendBuffer(body, resultHandler)
            else -> client.get(target.port, target.host, target.path)
        }
    }

    private fun onSuccess(input: Message<KEvent>, response: HttpResponse<Buffer>, interactive: Boolean = false) {
        val responseBody = response.bodyAsBuffer() ?: Buffer.buffer()
        val responseContentType = ContentTypeUtils.getContentType(response.getHeader("Content-Type"))
        log.debug { "Handle SUCCESS[${response.statusCode()}]: body size ${responseBody.length()}, contentType: $responseContentType" }

        val outEvent = KEvent(body = responseBody)
                .setContentType(ContentTypeUtils.getContentType(response.getHeader("Content-Type")))
                .setStatus(mapStatusCodeToStatus(response.statusCode(), Status.OK))
                .addHeader(Http.STATUS_MESSAGE_HEADER, response.statusMessage())
                .extendPath(input.body(), id())

        response.headers().names().forEach { headerName ->
            outEvent.addHeader("${Http.HEADER_PREFIX}${headerName}", response.getHeader(headerName))
        }

        if (interactive) {
            input.reply(outEvent)
        } else {
            publishOutputEvent(outEvent)
        }
    }

    private fun onFailure(input: Message<KEvent>, t: Throwable, interactive: Boolean = false) {
        val message = t.message ?: "Client Error"
        log.warn { "Handle FAILURE[${message}]" }
        val out = KEvent(message).setStatus(Status.INTERNAL_SERVER_ERROR).extendPath(input.body(), id())
        if (interactive) {
            input.fail(Status.INTERNAL_SERVER_ERROR.statusCode, message)
        } else {
            publishOutputEvent(out)
        }
    }

    private fun makeTargetURI(target: String, additionalPath: String): URI {
        val targetURL = URL(target)
        val targetURI = targetURL.toURI()
        var targetPath = targetURL.path ?: ""
        if (additionalPath.isNotBlank()) {
            targetPath += additionalPath
        }
        return URI(targetURI.scheme, null, targetURI.host, targetURI.port, targetPath, targetURI.query, targetURI.fragment)
    }

    private fun getHeaders(event: KEvent): MultiMap {
        val map = MultiMap.caseInsensitiveMultiMap()

        event.headers.entries
                .filter { it.key.startsWith(Http.HEADER_PREFIX) }
                .map { Pair(it.key.substringAfter(Http.HEADER_PREFIX), it.value.split(",")) }
                .flatMap { pair -> pair.second.map { Pair(pair.first, it) } }
                .filter { !it.second.isNullOrBlank() }
                .forEach { map.add(it.first, it.second) }

        return map
    }

    private fun queryParamSetter(key: String, value: String, request: HttpRequest<*>) {
        val decodedKey = URLDecoder.decode(key, "UTF-8")
        val decodedValue = URLDecoder.decode(value, "UTF-8")
        request.addQueryParam(decodedKey, decodedValue)
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}