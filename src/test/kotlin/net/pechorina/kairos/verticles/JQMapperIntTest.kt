package net.pechorina.kairos.verticles

import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.verticles.core.registerMessageCodecs
import net.pechorina.kairos.utils.deployStage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class JQMapperIntTest {

    @BeforeEach
    fun prepare() {
        TimeUnit.MILLISECONDS.sleep(500)
    }

    @Test
    fun mapJsonTest(vertx: Vertx, testContext: VertxTestContext) {
        val consumer = vertx.eventBus().consumer<KEvent>("test::jqmapper_out")
        consumer.handler { m: Message<KEvent> ->
            testContext.verify {
                val event = m.body()
                log.debug { "Received: ${event}" }
                assertThat(event.getPayloadAsString()).contains("Key1")
                testContext.completeNow()
            }
        }.completionHandler {
            log.debug { "Publishing event" }
            vertx.eventBus().publish(
                    "test::jqmapper_in",
                    KEvent(
                            json {
                                obj(
                                        "key" to "Key1",
                                        "value" to "value1"
                                )
                            }
                    )
            )
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}

        val stageConfig = """
---
namespace: "test"
instanceName: "JQMapper_01"
type: "net.pechorina.kairos.core.verticles.processors.JQMapper"
inputLanes:
  - type: EVENT
    name: "jqmapper_in"
outputLanes:
  - type: EVENT
    name: "jqmapper_out"
options:
  - section: main
    expression: ".key"
    outputType: TEXT
"""

        @BeforeAll
        @JvmStatic
        fun init(vertx: Vertx, testContext: VertxTestContext) {
            registerMessageCodecs(vertx)
            val definition = StageDefinition.fromYaml(stageConfig)
            deployStage(definition, vertx, testContext)
        }
    }
}