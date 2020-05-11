package net.pechorina.kairos.verticles

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.http.Http
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.verticles.core.registerMessageCodecs
import net.pechorina.kairos.utils.deployStage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class WebClientIntTest {

    val PORT = 8089
    var mockServer: HttpServer? = null

    @BeforeEach
    fun prepare() {
        TimeUnit.MILLISECONDS.sleep(200)
    }

    @Test
    fun testGET(vertx: Vertx, testContext: VertxTestContext) {
        if (mockServer == null) initMockServer(vertx)
        val event = KEvent()
        event.addHeader(Http.METHOD_HEADER, "GET")
        event.addHeader(Http.TARGET_HEADER, "http://localhost:${PORT}/api/items/5")

        vertx.eventBus().request<KEvent>("test::in1", event) {
            testContext.verify {
                assertThat(it.succeeded()).isTrue()
                val event = it.result().body()
                log.debug { "Received: ${event}" }
                assertThat(event.getStatus()).isEqualTo(Status.OK)
                assertThat(event.getContentType()).isEqualTo(ContentType.JSON)
                assertThat(event.body.length()).isEqualTo(10)
                testContext.completeNow()
            }
        }
    }

    @Test
    fun testGETComplex(vertx: Vertx, testContext: VertxTestContext) {
        if (mockServer == null) initMockServer(vertx)
        val event = KEvent()
        event.addHeader(Http.METHOD_HEADER, "GET")
        event.addHeader(Http.TARGET_HEADER, "http://localhost:${PORT}/api/tags/aaa?secret=CCC&desc=BBB&qty=3")

        vertx.eventBus().request<KEvent>("test::in1", event) {
            testContext.verify {
                assertThat(it.succeeded()).isTrue()
                val event = it.result().body()
                log.debug { "Received: ${event}" }
                assertThat(event.getStatus()).isEqualTo(Status.OK)
                assertThat(event.getContentType()).isEqualTo(ContentType.JSON)
                val body = event.getPayloadAsJsonObject()
                assertThat(body.getString("tag")).isEqualTo("aaa")
                assertThat(body.getString("desc")).isEqualTo("BBB")
                assertThat(body.getString("qty")).isEqualTo("3")
                assertThat(body.getString("secret")).isEqualTo("CCC")
                testContext.completeNow()
            }
        }
    }

    @Test
    fun testHEAD(vertx: Vertx, testContext: VertxTestContext) {
        if (mockServer == null) initMockServer(vertx)
        val event = KEvent()
        event.addHeader(Http.METHOD_HEADER, "HEAD")
        event.addHeader(Http.TARGET_HEADER, "http://localhost:${PORT}/api/items/6")

        vertx.eventBus().request<KEvent>("test::in1", event) {
            testContext.verify {
                assertThat(it.succeeded()).isTrue()
                val event = it.result().body()
                log.debug { "Received: ${event}" }
                assertThat(event.getStatus()).isEqualTo(Status.OK)
                assertThat(event.getContentType()).isEqualTo(ContentType.JSON)
                assertThat(event.body.length()).isEqualTo(0)
                testContext.completeNow()
            }
        }
    }

    @Test
    fun testDELETE(vertx: Vertx, testContext: VertxTestContext) {
        if (mockServer == null) initMockServer(vertx)
        val event = KEvent()
        event.addHeader(Http.METHOD_HEADER, "DELETE")
        event.addHeader(Http.TARGET_HEADER, "http://localhost:${PORT}/api/items/6")

        vertx.eventBus().request<KEvent>("test::in1", event) {
            testContext.verify {
                assertThat(it.succeeded()).isTrue()
                val event = it.result().body()
                log.debug { "Received: ${event}" }
                assertThat(event.getStatus()).isEqualTo(Status.OK)
                assertThat(event.getContentType()).isEqualTo(ContentType.JSON)
                assertThat(event.body.length()).isEqualTo(0)
                testContext.completeNow()
            }
        }
    }

    @Test
    fun testPOST(vertx: Vertx, testContext: VertxTestContext) {
        if (mockServer == null) initMockServer(vertx)

        val body = JsonObject()
                .put("id", 7)
                .put("name", "Item7")

        val event = KEvent(body)
        event.addHeader(Http.METHOD_HEADER, "POST")
        event.addHeader(Http.TARGET_HEADER, "http://localhost:${PORT}/api/items/7")

        vertx.eventBus().request<KEvent>("test::in1", event) {
            testContext.verify {
                assertThat(it.succeeded()).isTrue()
                val event = it.result().body()
                log.debug { "Received: ${event}" }
                assertThat(event.getStatus()).isEqualTo(Status.OK)
                assertThat(event.getContentType()).isEqualTo(ContentType.JSON)
                assertThat(event.getPayloadAsJsonObject()).isEqualTo(body)
                testContext.completeNow()
            }
        }
    }

    fun initMockServer(vertx: Vertx) {
        var server = vertx.createHttpServer()
        var router = Router.router(vertx)
        router.route().handler(ResponseTimeHandler.create())
        router.route().handler(LoggerHandler.create())
        router.route().handler(BodyHandler.create())
        router.route(HttpMethod.GET, "/api/items/:itemId").handler { rc ->
            val result = JsonObject()
                    .put("id", rc.request().getParam("itemId"))
            rc.response().putHeader("content-type", "application/json")
            rc.response().end(result.encode())
        }

        router.route(HttpMethod.DELETE, "/api/items/:itemId").handler { rc ->
            rc.response().putHeader("content-type", "application/json")
            rc.response().statusCode = 200
            rc.response().end()
        }

        router.route(HttpMethod.HEAD, "/api/items/:itemId").handler { rc ->
            rc.response().putHeader("content-type", "application/json")
            rc.response().statusCode = 200
            rc.response().end()
        }

        router.route(HttpMethod.POST, "/api/items/:itemId").handler { rc ->
            val body = rc.bodyAsJson
            rc.response().putHeader("content-type", "application/json")
            rc.response().end(body.toBuffer())
        }

        router.route(HttpMethod.PUT, "/api/items/:itemId").handler { rc ->
            val body = rc.bodyAsJson
            rc.response().putHeader("content-type", "application/json")
            rc.response().end(body.toBuffer())
        }

        router.route(HttpMethod.GET, "/api/tags/:tag").handler { rc ->
            val result = JsonObject()
                    .put("tag", rc.pathParam("tag"))
            rc.queryParams().entries().forEach { param -> result.put(param.key, param.value) }

            rc.response().putHeader("content-type", "application/json")
            rc.response().end(result.encode())
        }

        server.requestHandler(router).listen(PORT)
        registerMessageCodecs(vertx)
        log.debug { "Server is ready at $PORT" }
        this.mockServer = server
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}

        val stageConfig = """
---
namespace: "test"
instanceName: "WebClient_01"
type: "net.pechorina.kairos.core.verticles.http.WebClient"
inputLanes:
  - type: INTERACTIVE
    name: "in1"
options: []
"""

        @BeforeAll
        @JvmStatic
        fun init(vertx: Vertx, testContext: VertxTestContext) {
            val definition = StageDefinition.fromYaml(stageConfig)
            deployStage(definition, vertx, testContext)
        }
    }
}