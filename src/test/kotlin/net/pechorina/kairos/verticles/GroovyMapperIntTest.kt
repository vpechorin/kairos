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
class GroovyMapperIntTest {

    @BeforeEach
    fun prepare() {
        TimeUnit.MILLISECONDS.sleep(500)
    }

    @Test
    fun mapStringTest(vertx: Vertx, testContext: VertxTestContext) {
        val consumer = vertx.eventBus().consumer<KEvent>("test::groovymapper_out")
        consumer.handler { m: Message<KEvent> ->
            testContext.verify {
                val event = m.body()
                assertThat(event.getPayloadAsString()).isEqualTo("v")
                testContext.completeNow()
            }
        }.completionHandler {
            log.debug { "Publishing event" }
            vertx.eventBus().publish("test::groovymapper_in", KEvent(json { obj("k" to "v") }))
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}

        val mappperConfig = """
---
namespace: "test"
instanceName: "GroovyMapper_01"
type: "net.pechorina.kairos.core.verticles.processors.GroovyMapper"
inputLanes:
  - type: EVENT
    name: "groovymapper_in"
outputLanes:
  - type: EVENT
    name: "groovymapper_out"
options:
  - section: main
    expression: "input?.k.textValue()"
    outputType: TEXT
    contentType: JSON
"""
        val logConfig = """
---
namespace: "test"
instanceName: "EventLogSinkTest_01"
type: "net.pechorina.kairos.core.verticles.sinks.EventLogSink"
inputLanes:
  - type: EVENT
    name: "groovymapper_in"
  - type: EVENT
    name: "groovymapper_out"
"""

        @BeforeAll
        @JvmStatic
        fun init(vertx: Vertx, testContext: VertxTestContext) {
            registerMessageCodecs(vertx)
            deployStage(StageDefinition.fromYaml(mappperConfig), vertx, testContext)
            deployStage(StageDefinition.fromYaml(logConfig), vertx, testContext)
        }
    }
}