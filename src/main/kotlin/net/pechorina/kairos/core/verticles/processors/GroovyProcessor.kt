package net.pechorina.kairos.core.verticles.processors

import groovy.lang.GroovyShell
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.Section
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle

class GroovyProcessor : StageCoroutineVerticle() {

    lateinit var groovyExpression: String
    private val shell: GroovyShell = GroovyShell()

    suspend override fun start() {
        super.start()

        this.groovyExpression = definition.opts.section(Section.DEFAULT_SECTION_NAME).block().getString(ConfigKey.Expression.key, "")

        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
    }

    fun handler(message: Message<KEvent>) {
        launch(vertx.dispatcher()) {

            val inputEvent = message.body()

            val out = runBlocking {
                return@runBlocking process(message.body(), groovyExpression)
            }

            out.extendPath(inputEvent, id())
            publishOutputEvent(out)
        }
    }

    private fun process(event: KEvent, expression: String?): KEvent {
        shell.removeVariable("in")
        shell.removeVariable("out")
        try {
            shell.setVariable("in", event)
            shell.setVariable("id", id())
            return shell.evaluate(expression) as KEvent
        } catch (e: RuntimeException) {
            throw RuntimeException(String.format("Error using expression: [%s]", expression), e)
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
