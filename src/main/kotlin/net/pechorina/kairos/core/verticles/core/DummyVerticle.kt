package net.pechorina.kairos.core.verticles.core

import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle

class DummyVerticle : StageCoroutineVerticle() {

    override suspend fun start() {
        super.start()
        log.debug { "Started!" }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}