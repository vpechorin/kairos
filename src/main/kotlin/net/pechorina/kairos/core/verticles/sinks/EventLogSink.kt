package net.pechorina.kairos.core.verticles.sinks

import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle
import java.util.concurrent.atomic.AtomicLong

class EventLogSink : StageCoroutineVerticle() {

    val counter = AtomicLong(0L)

    suspend override fun start() {
        log.debug { "Starting EventLogSink" }
        super.start()
        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
        log.debug { "Started EventLogSink" }
    }

    fun handler(message: Message<KEvent>) {
        launch(vertx.dispatcher()) {
            val c = counter.incrementAndGet()
            val event = message.body()
            if (event != null) {
                log.info("Event[{}]:\n{}", c, event)
            }
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
