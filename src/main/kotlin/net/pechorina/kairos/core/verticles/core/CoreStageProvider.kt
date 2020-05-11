package net.pechorina.kairos.core.verticles.core

import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.utils.ClassUtils
import net.pechorina.kairos.core.utils.ResourceUtils
import net.pechorina.kairos.core.utils.stackTraceString
import net.pechorina.kairos.core.verticles.StageVerticle
import java.io.IOException
import java.io.UncheckedIOException

class CoreStageProvider : StageVerticle() {

    override fun start() {
        super.start()
        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
        subscribe(IOLaneType.INTERACTIVE, Handler { this.replyHandler(it) })
    }

    private fun replyHandler(message: Message<KEvent>) {
        val event = message.body()
        try {
            val outputEvent = KEvent(JsonArray(getStages())).extendPath(event, id()).setStatus(Status.OK)
            message.reply(outputEvent)
        } catch (t: Throwable) {
            message.fail(500, t.message)
        }
    }

    private fun handler(message: Message<KEvent>) {
        val event = message.body()
        try {
            val outputEvent = KEvent(JsonArray(getStages())).extendPath(event, id()).setStatus(Status.OK)
            publishOutputEvent(outputEvent)
        } catch (t: Throwable) {
            publishOutputEvent(KEvent(stackTraceString(t)).extendPath(event, id()).setStatus(Status.INTERNAL_SERVER_ERROR)
            )
        }
    }

    private fun getStages(): List<StageDefinition> {
        val list = ResourceUtils.getBootResources()
        return list.map { loadStage(it) }
    }

    fun loadStage(resource: String): StageDefinition {
        val classLoader = ClassUtils.defaultClassLoader
        try {
            classLoader?.getResourceAsStream(resource)
                    .use { inputStream ->
                        return StageDefinition.fromYamlInputStream(resource, inputStream, config())
                    }
        } catch (e: IOException) {
            throw UncheckedIOException("Can't read stage [$resource]", e)
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
