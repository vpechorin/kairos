package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.Handler
import io.vertx.core.Vertx
import mu.KLogger
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class EventWindow<T>(
        val vertx: Vertx,
        slotNames: Collection<String>,
        maxElements: Int?,
        timeWindow: Long?,
        val completionHandler: Handler<Map<String, T>>) {

    private val data = ConcurrentHashMap<String, T>()
    private val maxElements = AtomicInteger(0)
    private val timeWindow = AtomicLong()
    private val closed = AtomicBoolean(false)
    private val putLock = Any()
    private val timerId = AtomicLong()

    private val slots = ConcurrentHashMap<String, EventQueue<T>>()

    private val isFull: Boolean
        get() = data.size >= maxElements.get()

    init {

        if (maxElements != null) this.maxElements.set(maxElements)
        if (timeWindow != null) {
            this.timeWindow.set(timeWindow)
            setTimer()
        }

        slotNames.forEach { slotName ->
            EventQueue<T>(
                    slotName,
                    Handler { event -> this.onNewEvent(slotName, event) }
            )
        }
    }

    private fun setTimer() {
        timerId.set(
                this.vertx.setTimer(this.timeWindow.get()) { this.onTimeExpire() }
        )
    }

    private fun onTimeExpire() {
        log.debug("Timeout, closing window")
        this.closed.set(true)
        this.completionHandler.handle(data)
    }

    fun put(name: String, event: T) {
        data[name] = event
    }

    private fun checkState() {
        if (isFull) {
            log.debug("Full, closing window")
            this.closed.set(true)
            vertx.cancelTimer(timerId.get())
            this.completionHandler.handle(data)
        }
    }

    fun onNewEvent(slotName: String, event: T) {
        synchronized(putLock) {
            if (!closed.get() && isSlotEmpty(slotName)) {
                val queue = getQueue(slotName)
                queue.poll()?.let {
                    log.debug("Received data: {}", it)
                    data[slotName] = it
                }
            } else {
                log.debug("Closed: {}, SlotEmpty:{}", closed.get(), isSlotEmpty(slotName))
            }
        }
        checkState()
    }

    fun getQueue(slotName: String): EventQueue<T> {
        return slots[slotName]!!
    }

    fun isSlotEmpty(name: String): Boolean {
        return !data.containsKey(name)
    }

    fun reset() {
        log.debug("Reset")
        this.data.clear()
        this.closed.set(false)
        setTimer()
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
