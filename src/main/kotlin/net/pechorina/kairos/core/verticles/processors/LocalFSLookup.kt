package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.types.mapNameToContentType
import net.pechorina.kairos.core.verticles.StageVerticle
import java.nio.file.Path
import java.nio.file.Paths

class LocalFSLookup : StageVerticle() {

    lateinit var fs: FileSystem
    lateinit var volume: Path
    lateinit var outputType: ContentType

    override fun start() {
        super.start()

        this.volume = Paths.get(
                definition.opts.section(ConfigKey.FS.key).block().getString(ConfigKey.Path.key, "/tmp/" + System.currentTimeMillis())
        )

        this.fs = vertx.fileSystem()

        this.outputType = mapNameToContentType(
                definition.opts.section(ConfigKey.FS.key).block().getString(ConfigKey.OutputType.key, "TEXT"),
                ContentType.TEXT
        )

        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
        subscribe(IOLaneType.INTERACTIVE, Handler { this.replyHandler(it) })
    }

    private fun replyHandler(message: Message<KEvent>) {
        val event = message.body()
        // Event payload must be a String with the path to the file
        val filePath = event.getPayloadAsString()

        log.debug("requested file: ${filePath}")

        readFile(filePath).onComplete { result ->
            if (result.failed()) {
                log.warn("Can't read the file ${filePath}", result.cause())
                message.fail(500, "Can't read the file ${filePath}: ${result?.cause()?.message}")
            }

            message.reply(KEvent.makeKEvent(result.result(), outputType).extendPath(event, id()))
        }
    }

    private fun handler(message: Message<KEvent>) {
        val event = message.body()
        // Event payload must be a String with the path to the file
        val filePath = event.getPayloadAsString()

        log.debug("requested file: {}", filePath)

        readFile(filePath).onComplete { result ->
            if (result.failed()) {
                log.warn("Can't read the file ${filePath}", result.cause())
                return@onComplete
            }
            publishOutputEvent(KEvent.makeKEvent(result.result(), outputType).extendPath(event, id()))
        }
    }

    private fun readFile(filePath: String): Future<Buffer> {
        val file = Paths.get(filePath)
        val path = volume.resolve(file)

        val exists = Promise.promise<Boolean>()
        fs.exists(path.toString(), exists)

        return exists.future().compose { fileExists ->
            if (!fileExists) return@compose Future.failedFuture<Void>("File [${path}] was not found")
            Future.succeededFuture<Void>()
        }.compose {
            val future = Promise.promise<Buffer>()
            fs.readFile(path.toString(), future)
            future.future()
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
