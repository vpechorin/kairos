package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.Handler
import io.vertx.core.streams.Pump
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.IOLane
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.verticles.StageVerticle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class JoinProcessor : StageVerticle() {

    private val windowRef = AtomicReference<EventWindow<*>>()
    private val pumps = ConcurrentHashMap<IOLane, Pump>()

    override fun start() {
        super.start()

        val inputLanes = getInputLanes(IOLaneType.EVENT)
        val onComplete = Handler<Map<String, KEvent>> { map -> log.debug("Done: ${map}") }

        val window = EventWindow(
                vertx,
                inputLanes.map { it.name },
                inputLanes.size,
                10000L,
                onComplete
        )

        windowRef.set(window)

        for (ioLane in inputLanes) {
            val queue = window.getQueue(ioLane.name)
            val consumer = vertx.eventBus().consumer<KEvent>(ioLane.name)
            val pump = Pump.pump(consumer.bodyStream(), queue)
            pump.start()
            pumps[ioLane] = pump
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
        private val DEFAULT_POLICY = "REQUIRE_ALL"
    }
}
