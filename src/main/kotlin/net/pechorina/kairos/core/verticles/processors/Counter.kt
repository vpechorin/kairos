package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.Section
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.utils.CoersionUtils
import net.pechorina.kairos.core.utils.RetryUtils.retryWithPredicate
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle
import java.util.concurrent.atomic.AtomicLong

class Counter : StageCoroutineVerticle() {

    var counter: AtomicLong = AtomicLong(0)
    var startCount: Long = 1
    var increment: Long = 1
    var storeLane: String = ""
    val INITIAL_VALUE_DELAY_MS: Long = 500

    suspend override fun start() {
        super.start()

        this.startCount = definition.opts.section(Section.DEFAULT_SECTION_NAME).block().getLong(ConfigKey.StartCount.key, 0)
        this.increment = definition.opts.section(Section.DEFAULT_SECTION_NAME).block().getLong(ConfigKey.Increment.key, 1)
        this.storeLane = definition.opts.section(Section.DEFAULT_SECTION_NAME).block().getString(ConfigKey.StoreLane.key, "UNDEFINED")

        counter.set(startCount)

        vertx.setTimer(INITIAL_VALUE_DELAY_MS) { retrieveInitialValue() }

        subscribe(IOLaneType.EVENT, "NEXT", Handler { this.handlerNext(it) })
        subscribe(IOLaneType.EVENT, "THIS", Handler { this.handlerThis(it) })
    }

    fun handlerNext(message: Message<KEvent>) {
        val value = this.counter.incrementAndGet()
        putValue(value)
        publishOutputEvent(
                KEvent(value).extendPath(message.body(), id())
        )
    }

    fun handlerThis(message: Message<KEvent>) {
        val v = this.counter.get()
        publishOutputEvent(
                KEvent(v).extendPath(message.body(), id())
        )
    }

    fun retrieveInitialValue() {
        launch(vertx.dispatcher()) {
            getInitialValue()?.let {
                log.debug { "Set initial counter value to ${it}" }
                counter.set(it)
            }
        }
    }

    suspend fun getInitialValue(): Long? {
        val predicate = fun(value: Long?): Boolean = value != null
        log.debug { "getting initial value for the counter `${definition.instanceName}`" }
        return retryWithPredicate(
                10,
                1500,
                3000,
                1.5,
                predicate
        ) { getValue() }
    }

    suspend fun getValue(): Long? {
        val key = "${definition.instanceName}/counter"
        log.debug { "getValue for the key[${key}]" }
        val options = DeliveryOptions().setSendTimeout(5000)
        val reply = awaitResult<Message<KEvent>> { handler ->
            vertx.eventBus().request(
                    getEventBusAddress("GET_${storeLane}"),
                    KEvent(key).addPath(id()),
                    options,
                    handler)
        }
        val event = reply.body()
        log.debug { "Response received: ${event}" }
        val map = if (event.getStatus() == Status.OK) {
            log.debug { "Found: ${event.getPayloadAsJsonObject()}" }
            event.getPayloadAsJsonObject().map
        } else {
            log.debug { "Not found, status: ${event.getStatus()}" }
            mapOf<String, Any>()
        }

        return CoersionUtils.getLong(map[key], startCount)
    }

    fun putValue(value: Long) {
        val key = "${definition.instanceName}/counter"

        vertx.eventBus().publish(
                getEventBusAddress("PUT_${storeLane}"),
                KEvent(
                        JsonObject.mapFrom(
                                json { obj(key to value) }
                        )
                )
        )
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }

}
