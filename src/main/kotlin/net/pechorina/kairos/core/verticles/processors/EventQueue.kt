package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.streams.WriteStream
import mu.KLogger
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedQueue

val log: KLogger = KotlinLogging.logger {}

class EventQueue<T>(val name: String, val handler: Handler<T>? = null) : WriteStream<T> {

    private val queue = ConcurrentLinkedQueue<T>()
    private var drainHandler: Handler<Void>? = null
    private var exceptionHandler: Handler<Throwable>? = null
    private var writeQueueMaxSize = DEFAULT_QUEUE_SIZE

    val isEmpty: Boolean
        get() = queue.isEmpty()

    fun hasElements(): Boolean {
        return queue.peek() != null
    }

    fun poll(): T? {
        return queue.poll()
    }

    fun size(): Int {
        return queue.size
    }

    private fun executeHandler(event: T) {
        log.debug("executeHandler: {}", event)
        try {
            handler?.handle(event)
        } catch (e: Exception) {
            val t = RuntimeException(String.format("[%s] Error handling event", name), e)
            exceptionHandler?.handle(t)
        }
    }

    override fun exceptionHandler(handler: Handler<Throwable>): WriteStream<T> {
        this.exceptionHandler = handler
        return this
    }

    fun doWrite(event: T): T {
        log.debug("write: {}", event)
        this.queue.add(event)
        executeHandler(event)
        checkQueue()
        return event
    }

    override fun write(event: T): WriteStream<T> {
        doWrite(event)
        return this
    }

    private fun checkQueue() {
        val queueSize = queue.size
        if (queueSize == 0) return
        val fillRatio = if (queueSize == 0) 0.0 else writeQueueMaxSize.toDouble() / queueSize.toDouble()
        log.debug("EventQueue[{}] fill level: {}", name, fillRatio)
        if (fillRatio <= RESTART_CONSUMING_LEVEL && drainHandler != null) {
            drainHandler?.handle(null)
        }
    }

    override fun end() {

    }

    override fun write(data: T, handler: Handler<AsyncResult<Void>>?): WriteStream<T> {
        doWrite(data)
        handler.run {  }
        return this
    }

    override fun end(handler: Handler<AsyncResult<Void>>?) {
        handler.run {  }
    }

    override fun setWriteQueueMaxSize(maxSize: Int): WriteStream<T> {
        this.writeQueueMaxSize = maxSize
        return this
    }

    override fun writeQueueFull(): Boolean {
        return queue.size >= this.writeQueueMaxSize
    }

    override fun drainHandler(handler: Handler<Void>): WriteStream<T>? {
        this.drainHandler = handler
        return null
    }

    companion object {
        private val DEFAULT_QUEUE_SIZE = 100
        private val RESTART_CONSUMING_LEVEL = 0.6f
    }
}
