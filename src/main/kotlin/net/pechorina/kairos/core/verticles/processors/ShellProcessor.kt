package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.verticles.StageVerticle
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ShellProcessor : StageVerticle() {

    private var executorService: ExecutorService? = null

    private val commands: List<String>
        get() {
            val commands = ArrayList<String>()
            val shell = definition.opts.section(ConfigKey.Shell.key).block().getString(ConfigKey.Execute.key, "/bin/sh")
            if (shell.toLowerCase().contains("sh")) {
                commands.add(shell)
                commands.add("-c")
                commands.add("--")
            }
            definition.opts.section(ConfigKey.Shell.key).block().getString(ConfigKey.Cmd.key)?.let {
                commands.add(it)
            }

            return commands
        }

    override fun start() {
        super.start()

        val poolSize = definition.opts.section(ConfigKey.Shell.key).block().getInteger(ConfigKey.PoolSize.key, 5)
        executorService = Executors.newFixedThreadPool(poolSize)

        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
    }

    private fun handler(message: Message<KEvent>) {
        executorService!!.submit { startProcess(message.body()) }
    }

    private fun startProcess(sourceEvent: KEvent) {
        val outputFile: File
        try {
            outputFile = File.createTempFile("shell-proc-out", ".tmp")
        } catch (e: IOException) {
            log.error("Unable to create TMP file", e)
            return
        }

        val commands = commands
        log.debug("Execute commands: [{}]", commands)
        val processBuilder = ProcessBuilder(commands)

        definition.opts.section(ConfigKey.Shell.key).block().getString(ConfigKey.WorkDir.key)?.let { dir ->
            processBuilder.directory(Paths.get(dir).toFile())
        }

        addEnvironment(processBuilder)

        processBuilder.redirectErrorStream(true)
        processBuilder.redirectOutput(ProcessBuilder.Redirect.to(outputFile))
        var process: Process? = null
        var errors: String? = null
        try {
            process = processBuilder.start()
        } catch (e: IOException) {
            log.warn("Process error", e)
            errors = e.message
        }

        val delay = definition.opts.section(ConfigKey.Shell.key).block().getLong(ConfigKey.Timeout.key, 600000L)
        var result = -1
        try {
            if (process!!.waitFor(delay, TimeUnit.MILLISECONDS)) {
                result = process.exitValue()
            } else {
                process.destroyForcibly()
            }
        } catch (e: InterruptedException) {
            errors = errors + "\n" + e.message
        }

        val outputEvent = createEvent(sourceEvent, result, outputFile, errors)

        publishOutputEvent(outputEvent)
    }

    private fun addEnvironment(processBuilder: ProcessBuilder) {
        val env = processBuilder.environment()
        val systemEnvironment = System.getenv()
        for (envName in systemEnvironment.keys) {
            env[envName] = systemEnvironment[envName]
        }

        definition.environment.entries
                .forEach { e -> env[e.key] = e.value }

    }

    private fun createEvent(sourceEvent: KEvent, exitCode: Int, processOutput: File, errors: String?): KEvent {
        val path = ArrayList(sourceEvent.path)
        path.add(id())
        var chunks: MutableList<String> = arrayListOf()
        errors?.let { chunks.add(it) }

        try {
            val outputString = processOutput.readText(Charset.defaultCharset())
            log.debug { "File content: ${outputString}" }
            chunks.add(outputString)
            processOutput.delete()
        } catch (e: IOException) {
            log.error("Can't read process output file", e)
        }

        val body = chunks.joinToString("\n").trim()

        val event = KEvent(body).extendPath(sourceEvent, id())
        event.addHeader("exitCode", exitCode.toString())
        return event
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
