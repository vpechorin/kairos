package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonArray
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.verticles.StageVerticle
import java.nio.file.Path
import java.nio.file.Paths

class LocalFSReadDir : StageVerticle() {

    lateinit var fs: FileSystem
    lateinit var volume: Path

    override fun start() {
        super.start()

        this.volume = Paths.get(
                definition.opts.section(ConfigKey.FS.key).block().getString(ConfigKey.Path.key, "/tmp/" + System.currentTimeMillis())
        )

        this.fs = vertx.fileSystem()

        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
        subscribe(IOLaneType.INTERACTIVE, Handler { this.replyHandler(it) })
    }

    private fun replyHandler(message: Message<KEvent>) {
        val event = message.body()
        var directory = event.getPayloadAsString()

        if (directory.isNullOrEmpty()) {
            log.warn("Empty path")
            message.fail(500, "Empty path")
            return
        }

        log.debug("requested path: [${directory}]")

        val filePath = Paths.get(directory)
        val path = volume.resolve(filePath)

        log.debug { "Path to read: [${path}]" }
        listDir(path)
                .onSuccess { message.reply(KEvent(JsonArray(it)).extendPath(event, id())) }
                .onFailure { message.fail(500, "Can't read the directory ${path}: ${it?.cause?.message}") }
    }

    private fun handler(message: Message<KEvent>) {
        val event = message.body()
        var directory = event.getPayloadAsString()

        if (directory.isNullOrEmpty()) {
            log.warn("Empty path")
            message.fail(500, "Empty path")
            return
        }

        log.debug("requested path: [${directory}]")

        log.debug("path: [{}]", directory)
        val path = Paths.get(directory)

        listDir(path)
                .onSuccess { publishOutputEvent(KEvent(JsonArray(it)).extendPath(event, id())) }
                .onFailure { log.warn("Can't list the directory ${path}", it.cause) }
    }

    private fun listDir(path: Path): Future<List<String>> {
        val promise = Promise.promise<List<String>>()
        fs.readDir(path.toString()) { result ->
            if (result.succeeded()) {
                promise.complete(result.result()?.filterNotNull()?.map { toDirName(it) } ?: emptyList())
            } else {
                promise.fail(result.cause())
            }
        }

        return promise.future()
    }

    private fun toDirName(fullPath: String): String {
        val p = Paths.get(fullPath)
        return p.fileName.toString()
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
