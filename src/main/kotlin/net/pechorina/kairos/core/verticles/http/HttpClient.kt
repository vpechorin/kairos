package net.pechorina.kairos.core.verticles.http

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.Block
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.http.ContentTypeUtils
import net.pechorina.kairos.core.http.Http
import net.pechorina.kairos.core.mappers.GroovyObjectMapper
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle

class HttpClient : StageCoroutineVerticle() {

    lateinit var client: WebClient
    lateinit var groovyObjectMapper: GroovyObjectMapper

    var host: String? = null
    var port: Int? = null
    var path: String = "/"
    var contentType: String? = null
    var acceptType: String? = null

    var successOutputLane: String? = null
    var failureOutputLane: String? = null
    var badRequestOutputLane: String? = null
    var unauthorizedOutputLane: String? = null
    var forbiddenOutputLane: String? = null
    var notFoundOutputLane: String? = null

    var webClientOptions: WebClientOptions = io.vertx.kotlin.ext.web.client.webClientOptionsOf()

    override suspend fun start() {
        super.start()
        this.groovyObjectMapper = GroovyObjectMapper()
        val configBlock = definition.opts.section(ConfigKey.HttpClient.key).block()
        val webClientOptionsBlock = definition.opts.section(ConfigKey.WebClientOptions.key).block()

        val httpMethod = configBlock.getString(ConfigKey.HttpMethod.key, "GET")

        this.successOutputLane = configBlock.getString(ConfigKey.SuccessLane.key)
        this.failureOutputLane = configBlock.getString(ConfigKey.FailureLane.key)
        this.badRequestOutputLane = configBlock.getString(ConfigKey.Error400Lane.key)
        this.unauthorizedOutputLane = configBlock.getString(ConfigKey.Error401Unauthorized.key)
        this.forbiddenOutputLane = configBlock.getString(ConfigKey.Error403Forbidden.key)
        this.notFoundOutputLane = configBlock.getString(ConfigKey.Error404NotFound.key)

        readConfig(configBlock)

        this.webClientOptions = readOptions(webClientOptionsBlock)
        this.client = WebClient.create(vertx, this.webClientOptions)

        when (httpMethod) {
            "GET" -> subscribe(IOLaneType.EVENT, Handler { this.handlerGet(it) })
            "POST" -> subscribe(IOLaneType.EVENT, Handler { this.handlerPost(it) })
            "PUT" -> subscribe(IOLaneType.EVENT, Handler { this.handlerPut(it) })
            "DELETE" -> subscribe(IOLaneType.EVENT, Handler { this.handlerDelete(it) })
        }
    }

    fun readConfig(configBlock: Block) {
        this.contentType = configBlock.getString(ConfigKey.ContentType.key, "text/plain")
        this.acceptType = configBlock.getString(ConfigKey.AcceptType.key)
        this.host = configBlock.getString(ConfigKey.Host.key)
        this.port = configBlock.getInteger(ConfigKey.Port.key)
        this.path = configBlock.getString(ConfigKey.Path.key, "/")
    }

    fun readOptions(configBlock: Block): WebClientOptions {
        try {
            return WebClientOptions(
                    JsonObject.mapFrom(configBlock.options)
            )
        } catch (e: Throwable) {
            log.error(e) { "Error reading WebClientConfig" }
        }
        return WebClientOptions()
    }

    private fun createEvent(sourceEvent: KEvent, httpResponse: HttpResponse<Buffer>): KEvent {

        val contentTypeRaw = httpResponse.getHeader("Content-Type")

        val contentType = if (contentTypeRaw != null)
            ContentTypeUtils.getContentType(contentTypeRaw)
        else
            ContentType.TEXT

        var body = httpResponse.bodyAsBuffer()

        val event = if (body == null)
            KEvent()
        else
            KEvent(body = body)
                    .setContentType(contentType)
                    .extendPath(sourceEvent, id())

        httpResponse.headers().names().forEach { headerName ->
            event.addHeader("${Http.HEADER_PREFIX}${headerName}", httpResponse.getHeader(headerName))
        }

        return event
    }

    fun handlerGet(message: Message<KEvent>) {
        val event = message.body()
        //val preparedPath = groovyObjectMapper.mapToString(path, event.payload)
        val preparedPath = path

        log.debug { "Host:${host}, Port:${port}, Path:${path}" }

        val request = when {
            (port != null && host != null) -> client.get(port!!, host, preparedPath)
            (port == null && host != null) -> client.get(host, preparedPath)
            else -> client.get(preparedPath)
        }

        request.ssl(webClientOptions.isSsl)
        if (acceptType != null) request.putHeader("Accept", acceptType)
        request.send { ar ->
            if (ar.succeeded()) {
                publishOutputEvent(createEvent(event, ar.result()))
            } else {
                log.debug { "Something went wrong ${ar.cause().message}" }
            }
        }
    }

    fun handlerPost(message: Message<KEvent>) {

    }

    fun handlerPut(message: Message<KEvent>) {

    }

    fun handlerDelete(message: Message<KEvent>) {

    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}