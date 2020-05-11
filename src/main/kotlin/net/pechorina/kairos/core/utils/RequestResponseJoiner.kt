package net.pechorina.kairos.core.utils

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import mu.KotlinLogging
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.ContentType
import java.util.Objects

class RequestResponseJoiner(requestAddress: String, responseAddress: String, timeoutMs: Long, vertx: Vertx) {

    private val requestAddress: String
    private val responseAddress: String
    private var timeoutMs: Long = 3600000
    private val vertx: Vertx
    private var timerId: Long? = null

    init {
        log.debug("Join response to request {} <-> {}, timeout: {} ms", requestAddress, responseAddress, timeoutMs)
        this.requestAddress = Objects.requireNonNull(requestAddress)
        this.responseAddress = Objects.requireNonNull(responseAddress)
        if (timeoutMs != 0L) this.timeoutMs = timeoutMs
        this.vertx = Objects.requireNonNull(vertx)
    }

    constructor(requestAddress: String, responseAddress: String, vertx: Vertx) : this(requestAddress, responseAddress, 0L, vertx)

    fun run(requestBody: String, originId: String): Future<KEvent> {
        val resultPromise = Promise.promise<KEvent>()

        val responseFuture = responseFuture(requestBody, originId)
        val timeoutFuture = timeoutFuture()

        responseFuture.onSuccess { resultPromise.complete(it) }
                .onFailure { resultPromise.fail(it) }
                .onComplete { vertx.cancelTimer(timerId!!) }

        timeoutFuture.onComplete {
            vertx.cancelTimer(timerId!!)
            resultPromise.fail("Timeout")
        }

        return resultPromise.future()
    }

    private fun responseFuture(requestBody: String, originId: String): Future<KEvent> {
        val promise = Promise.promise<KEvent>()

        val newEvent = KEvent(requestBody)
                .addPath(originId)

        val consumer = vertx.eventBus().consumer<KEvent>(responseAddress)

        consumer.handler { message ->
            val event = message.body()
            val responseMatches = event.getHeader(KEvent.REPLY_TO_HEADER) == newEvent.id
            log.debug("Received response[{}]: {}", responseMatches, event)
            if (responseMatches) {
                promise.complete(event)
                if (consumer.isRegistered) consumer.unregister()
            }
        }
        consumer.completionHandler {
            log.debug("Sending request to {}: {}", requestAddress, newEvent)
            vertx.eventBus().publish(requestAddress, newEvent)
        }

        return promise.future()
    }


    private fun timeoutFuture(): Future<Void> {
        val f = Promise.promise<Void>()
        this.timerId = vertx.setTimer(timeoutMs) { f.fail("timeout") }
        return f.future()
    }

    companion object {
        val log = KotlinLogging.logger {}
    }
}
