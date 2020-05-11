package net.pechorina.kairos.core.verticles.sinks

import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.verticles.StageVerticle
import java.nio.file.Path
import java.nio.file.Paths

class LocalFSSink : StageVerticle() {

    private lateinit var volume: Path

    override fun start() {
        super.start()

        volume = Paths.get(
                definition.opts.section(ConfigKey.FS.key).block().getString(ConfigKey.Path.key, "/tmp/${System.currentTimeMillis()}")
        )


        vertx.fileSystem().mkdirs(volume.toString()) {
            if (it.succeeded()) {
                log.debug { "${volume} - created" }
            } else {
                log.error { "Unable to create a directory ${volume} - ${it.cause()}" }
            }
        }

        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
    }

    private fun handler(message: Message<KEvent>) {
        val event = message.body()
        val filePath = event.getFilePath() ?: throw RuntimeException("File name is not found in the event header")
        val target = volume.resolve(Paths.get(filePath))

        val promise = Promise.promise<Void>()
        vertx.fileSystem().mkdirs(target.parent.toString(), promise)
        promise.future().onComplete {
            if (it.failed()) {
                log.error { "Unable to create a directory ${target.parent} - ${it.cause()}" }
            }
            saveFile(target, event.body)
        }
    }

    private fun saveFile(target: Path, buffer: Buffer) {
        vertx.fileSystem().writeFile(target.toString(), buffer) { result ->
            if (result.succeeded()) {
                log.debug { "${target} - ${buffer.length()} - saved" }
            } else {
                log.error { "Error saving ${target} - ${result.cause()}" }
            }
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
