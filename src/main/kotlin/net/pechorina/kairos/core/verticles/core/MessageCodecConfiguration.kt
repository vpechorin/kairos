package net.pechorina.kairos.core.verticles.core

import io.vertx.core.Vertx
import mu.KotlinLogging
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.codecs.KEventMessageCodec
import net.pechorina.kairos.core.codecs.StageDefinitionMessageCodec
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.utils.JsonMapper
import net.pechorina.kairos.core.verticles.MainVerticle

fun registerMessageCodecs(vertx: Vertx) {
    MainVerticle.log.info { "Register message codecs" }
    val eventBus = vertx.eventBus()

    eventBus.unregisterDefaultCodec(KEvent::class.java)
    eventBus.registerDefaultCodec(KEvent::class.java,
            KEventMessageCodec("KEventCodec")
    )
    MainVerticle.log.info { " KEvent codec registered" }

    eventBus.unregisterDefaultCodec(StageDefinition::class.java)
    eventBus.registerDefaultCodec(StageDefinition::class.java,
            StageDefinitionMessageCodec("StageDefinitionMessageCodec", JsonMapper.jsonObjectMapper))

    MainVerticle.log.info { " StageDefinition codec registered" }
    MainVerticle.log.info { "Codec registration completed" }
}

val log = KotlinLogging.logger {}