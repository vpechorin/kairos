package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLane
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.verticles.StageVerticle

class RequestResponseJoinProcessor : StageVerticle() {

    private lateinit var responseLane: IOLane
    private lateinit var requestLane: IOLane
    private var timeoutMs: Long? = 2000L
    private var timerId: Long? = null

    override fun start() {
        super.start()

        this.responseLane = getInputLanes(IOLaneType.EVENT).first()
        this.requestLane = getOutputLanes(IOLaneType.EVENT).first()

        this.timeoutMs = definition.opts
                .section("join")
                .block()
                .getLong(ConfigKey.Timeout.key, 2000L)

        subscribe(IOLaneType.INTERACTIVE, Handler { this.handler(it) })
    }

    private fun handler(message: Message<KEvent>) {
        log.debug("Incoming request: {}", message.body())
        val inEvent = message.body()

        val future = ask(inEvent)
        future
                .onSuccess { result ->
                    log.debug("Received reply: {}", result)
                    message.reply(result)
                }
                .onFailure { error ->
                    message.fail(500, error.message)
                }
    }

    private fun ask(sourceEvent: KEvent): Future<KEvent> {
        val resultPromise = Promise.promise<KEvent>()

        val responseFuture = responseFuture(sourceEvent)
        val timeoutFuture = timeoutFuture()

        responseFuture
                .onSuccess {
                    resultPromise.complete(it)
                    timerId?.let { timerId -> vertx.cancelTimer(timerId) }
                }
                .onFailure {
                    log.debug("Receiving failure: {}", it)
                    resultPromise.fail(it)
                    timerId?.let { timerId -> vertx.cancelTimer(timerId) }
                }

        timeoutFuture.onComplete {
            log.debug("Timeout!")
            timerId?.let { vertx.cancelTimer(it) }
            resultPromise.fail("Timeout")
        }

        return resultPromise.future()
    }

    private fun responseFuture(sourceEvent: KEvent): Future<KEvent> {
        val promise = Promise.promise<KEvent>()

        val newEvent = sourceEvent.makeChild(id())

        val consumer = vertx.eventBus()
                .consumer<KEvent>(getEventBusAddress(responseLane))

        consumer.handler { message ->
            val event = message.body()
            val responseMatches = event.path.contains(id())

            log.debug("Received response[{}]: {}", responseMatches, event)
            if (responseMatches) {
                promise.complete(event)
                if (consumer.isRegistered) consumer.unregister()
            }
        }

        consumer.completionHandler { sendRequest(newEvent) }

        return promise.future()
    }

    private fun sendRequest(event: KEvent) {
        log.debug("Sending request to {}: {}", getEventBusAddress(requestLane), event)
        vertx.eventBus().publish(getEventBusAddress(requestLane), event)
    }

    private fun timeoutFuture(): Future<Void> {
        val promise = Promise.promise<Void>()
        this.timerId = vertx.setTimer(timeoutMs!!) { promise.fail("timeoutMs") }
        return promise.future()
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
