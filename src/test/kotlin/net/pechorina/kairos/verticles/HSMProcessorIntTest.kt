package net.pechorina.kairos.verticles

import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.verticles.core.registerMessageCodecs
import net.pechorina.kairos.utils.deployStage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class HSMProcessorIntTest {

    @BeforeEach
    fun prepare() {
        TimeUnit.MILLISECONDS.sleep(500)
    }

    @Test
    fun simpleRunTest(vertx: Vertx, testContext: VertxTestContext) {
        val consumer = vertx.eventBus().consumer<KEvent>("test::out1")
        consumer.handler { m: Message<KEvent> ->
            testContext.verify {
                val event = m.body()
                log.debug { "Received: ${event}" }
                assertThat(event.getContentType()).isEqualTo(ContentType.INT)
                assertThat(event.getPayloadAsInt()).isEqualTo(1)
                testContext.completeNow()
            }
        }.completionHandler {
            log.debug { "Publishing event" }
            vertx.eventBus().publish("test::in1", KEvent())
            vertx.eventBus().publish("test::in2", KEvent())
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}

        val stageConfig = """
---
namespace: "test"
instanceName: "GroovyMapper_01"
type: "net.pechorina.kairos.core.verticles.processors.HSMProcessor"
inputLanes:
  - type: EVENT
    name: "in1"
  - type: EVENT
    name: "in2"
outputLanes:
  - type: EVENT
    name: "out1"
  - type: EVENT
    name: "out2"
options:
    - section: states
      state: S0
      initialState: true
    - section: states
      state: S1
      initialState: true
    - section: states
      state: S2
      entryAction: out1::1::int
    - section: events
      lane: in1
      event: E1
    - section: events
      lane: in2
      event: E2
    - section: transitions
      source: S0
      target: S1
      event: E1
    - section: transitions
      source: S1
      target: S2
      event: E2
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