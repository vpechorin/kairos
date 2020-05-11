package net.pechorina.kairos.verticles

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.verticles.core.registerMessageCodecs
import net.pechorina.kairos.utils.deployStage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Random
import java.util.UUID
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class KeyValueStoreIntTest {

    @BeforeEach
    fun prepare() {
        TimeUnit.MILLISECONDS.sleep(500)
    }

    @Test
    fun get_ExpectNotFound(vertx: Vertx, testContext: VertxTestContext) {
        log.debug { "Publishing event" }

        vertx.eventBus().request<KEvent>(
                "test::GET_kv",
                KEvent(UUID.randomUUID().toString())
        ) { ar ->
            if (ar.failed()) {
                testContext.failNow(ar.cause())
                return@request
            }

            val event = ar.result().body()
            log.debug { "Received: ${event}" }
            assertThat(event.getStatus()).isEqualTo(Status.NOT_FOUND)
            testContext.completeNow()
        }
    }

    @Test
    fun putThenGetTest(vertx: Vertx, testContext: VertxTestContext) {
        log.debug { "Publishing event" }
        vertx.eventBus().publish(
                "test::PUT_kv",
                KEvent(json { obj("k1" to "v1") })
        )

        vertx.eventBus().request<KEvent>(
                "test::GET_kv",
                KEvent("k1")
        ) { ar ->
            if (ar.failed()) {
                testContext.failNow(ar.cause())
                return@request
            }

            val event = ar.result().body()
            log.debug { "Received: ${event}" }
            assertThat(event.getPayloadAsJsonObject()?.map).containsAllEntriesOf(mapOf("k1" to "v1"))
            testContext.completeNow()
        }
    }

    @Test
    fun putThenDeleteTest(vertx: Vertx, testContext: VertxTestContext) {
        log.debug { "Publishing event" }
        vertx.eventBus().publish(
                "test::PUT_kv",
                KEvent(json { obj("k2" to "v2") })
        )

        vertx.eventBus().publish(
                "test::DELETE_kv",
                KEvent("k2")
        )

        vertx.eventBus().request<KEvent>(
                "test::GET_kv",
                KEvent("k2")
        ) { ar ->
            if (ar.failed()) {
                testContext.failNow(ar.cause())
                return@request
            }

            log.debug { "Received: ${ar.result().body()}" }
            assertThat(ar.result().body().getStatus()).isEqualTo(Status.NOT_FOUND)
            testContext.completeNow()
        }
    }

    @Test
    fun updateKeyTest(vertx: Vertx, testContext: VertxTestContext) {
        log.debug { "Publishing event" }
        vertx.eventBus().publish(
                "test::PUT_kv",
                KEvent(json { obj("k3" to "v1") })
        )

        vertx.eventBus().publish(
                "test::PUT_kv",
                KEvent(json { obj("k3" to "v2") })
        )

        vertx.eventBus().request<KEvent>(
                "test::GET_kv",
                KEvent("k3")
        ) { ar ->
            if (ar.failed()) {
                testContext.failNow(ar.cause())
                return@request
            }

            log.debug { "Received: ${ar.result().body()}" }
            val event = ar.result().body()
            assertThat(event.getPayloadAsJsonObject()?.map).containsAllEntriesOf(mapOf("k3" to "v2"))
            testContext.completeNow()
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
        val seed = Random().nextInt(10000)
        val stageConfig = """
---
namespace: test
instanceName: KeyValueStore_01
type: net.pechorina.kairos.core.verticles.processors.KeyValueStore
inputLanes:
  - type: INTERACTIVE
    name: GET_kv
  - type: EVENT
    name: PUT_kv
  - type: EVENT
    name: DELETE_kv
options:
  - section: main
    path: ./build/teststore_${seed}
    store: test_01
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