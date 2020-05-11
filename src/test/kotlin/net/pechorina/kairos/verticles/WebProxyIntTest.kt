package net.pechorina.kairos.verticles

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.verticles.core.registerMessageCodecs
import net.pechorina.kairos.utils.deployStage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class WebProxyIntTest {

    val PORT = 8088
    var mockServer: HttpServer? = null

    @BeforeEach
    fun prepare() {
        TimeUnit.MILLISECONDS.sleep(200)
    }

    @Test
    fun callServerExpectProxyToMockServer(vertx: Vertx, testContext: VertxTestContext) {
        if (mockServer == null) initMockServer(vertx)
        val client = WebClient.create(vertx)

        val request = client.get(8087, "localhost", "/api/edge/tags/a123").`as`(BodyCodec.jsonObject())

        request.send {
            testContext.verify {
                assertThat(it.succeeded()).isTrue()
                val result = it.result()
                log.debug { "Headers: ${result.headers()}" }
                log.debug { "Body: ${result.body()}" }
                assertThat(result.statusCode()).isEqualTo(200)
                assertThat(result.getHeader("Content-Type")).isEqualTo("application/json")
                val body = result.body()
                assertThat(body.getString("tag")).isEqualTo("a123")
                assertThat(body.getString("status")).isEqualTo("XXX")
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

        router.route(HttpMethod.GET, "/api/tags/:tag").handler { rc ->
            val result = JsonObject()
                    .put("tag", rc.pathParam("tag"))
                    .put("status", "XXX")
            rc.queryParams().entries().forEach { param -> result.put(param.key, param.value) }

            rc.response().putHeader("content-type", "application/json")
            rc.response().end(result.encode())
        }

        server.requestHandler(router).listen(PORT)
        log.debug { "Server is ready at $PORT" }
        registerMessageCodecs(vertx);
        this.mockServer = server
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}

        val webClientStage = """
---
namespace: "test"
instanceName: "WebClient_01"
type: "net.pechorina.kairos.core.verticles.http.WebClient"
inputLanes:
  - type: INTERACTIVE
    name: "get_tag_io"
options: []
"""

        val webServerStage = """
---
namespace: "test"
instanceName: "WebServer_01"
type: "net.pechorina.kairos.core.verticles.http.HttpServerSource"
inputLanes:
  - type: INTERACTIVE
    name: "in1"
options:
  - section: http
    port: 8087
    
  - section: routes
    route: "/api/edge/tags/:tag"
    method: GET
    proxyPassEx: "http://localhost:8088/api/tags/{{ ctx.request().getParam('tag') }}"
    output: "get_tag_io"
    
outputLanes:
  - type: INTERACTIVE
    name: "get_tag_io"
"""

        @BeforeAll
        @JvmStatic
        fun init(vertx: Vertx, testContext: VertxTestContext) {
            deployStage(StageDefinition.fromYaml(webClientStage), vertx, testContext)
            deployStage(StageDefinition.fromYaml(webServerStage), vertx, testContext)
        }
    }
}