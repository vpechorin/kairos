package net.pechorina.kairos.verticles

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.verticles.core.registerMessageCodecs
import net.pechorina.kairos.utils.deployStages
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Random
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class CounterIntTest {

    @BeforeEach
    fun prepare() {
        TimeUnit.MILLISECONDS.sleep(1500)
    }

    @Test
    fun incrementCounter(vertx: Vertx, testContext: VertxTestContext) {

        vertx.eventBus().consumer<KEvent>(
                "test::COUNT_cnt1"
        ) {
            assertThat(it.body().getPayloadAsLong()).isEqualTo(101)
            testContext.completeNow()
        }

        log.debug { "Publishing event" }
        vertx.eventBus().publish(
                "test::NEXT_cnt1",
                KEvent()
        )
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
        val seed = Random().nextInt(10000)
        val kvStageConfig = """
---
namespace: "test"
instanceName: "KeyValueStore_02"
type: "net.pechorina.kairos.core.verticles.processors.KeyValueStore"
inputLanes:
  - type: INTERACTIVE
    name: "GET_kv2"
  - type: EVENT
    name: "PUT_kv2"
  - type: EVENT
    name: "DELETE_kv2"
options:
  - section: main
    path: "./build/teststore2_${seed}"
    store: test_02
"""
        val counterStageConfig = """
---
namespace: "test"
instanceName: "Counter_01"
type: "net.pechorina.kairos.core.verticles.processors.Counter"
inputLanes:
  - type: EVENT
    name: "NEXT_cnt1"
  - type: EVENT
    name: "THIS_cnt1"
outputLanes:
  - type: EVENT
    name: "COUNT_cnt1"
options:
  - section: main
    startCount: 100
    increment: 1
    storeLane: kv2
    inputType: "OBJECT"
    path: "./build/teststore2_${seed}"
    store: test_02
"""

        @BeforeAll
        @JvmStatic
        fun init(vertx: Vertx, testContext: VertxTestContext) {
            registerMessageCodecs(vertx)
            deployStages(
                    mutableListOf(StageDefinition.fromYaml(kvStageConfig), StageDefinition.fromYaml(counterStageConfig)),
                    vertx,
                    testContext
            )
        }
    }
}