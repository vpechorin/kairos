package net.pechorina.kairos.core.verticles.scheduling

import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.verticles.StageVerticle
import java.util.concurrent.atomic.AtomicLong

/*
---
namespace: "core"
instanceName: "Pulse_01"
type: "net.pechorina.kairos.core.verticles.scheduling.PeriodicTrigger"
options:
- section: timer
interval: 10000

outputLanes:
- type: EVENT
name: "out_pulse"

*/

class PeriodicTrigger : StageVerticle() {

    private val counter = AtomicLong(0L)

    override fun start() {
        super.start()
        val interval = definition.opts
                .section(ConfigKey.Timer.name)
                .block()
                .getLong(ConfigKey.Interval.key) ?: throw RuntimeException("Interval is not defined")

        log.debug { "Interval: ${interval} ms" }

        vertx.setPeriodic(interval) { _ ->
            val value = counter.incrementAndGet()
            publishOutputEvent(
                    KEvent(value).addPath(id())
            )
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
