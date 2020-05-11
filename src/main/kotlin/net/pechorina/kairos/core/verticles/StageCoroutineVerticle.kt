package net.pechorina.kairos.core.verticles

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.Constants
import net.pechorina.kairos.core.IOLane
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.events.KEvent
import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArrayList

private val ORIGIN_HEADER = "Origin"

abstract class StageCoroutineVerticle : CoroutineVerticle() {
    lateinit var definition: StageDefinition
    var namespace: String = Constants.DEFAULT_NAMESPACE
    val verticleConsumers: MutableList<MessageConsumer<KEvent>> = CopyOnWriteArrayList()

    override suspend fun start() {
        super.start()
        val definition = config.getJsonObject("definition")
        this.definition = StageDefinition.fromJsonObject(definition)
        this.namespace = this.definition.namespace
        log.debug { "[${namespace}] ${this.definition.type} - ${this.definition.instanceName} - STARTED" }
    }

    fun getOutputLanes(type: IOLaneType): List<IOLane> {
        return definition.outputLanes.filter { it.type == type }
    }

    fun getInputLanes(type: IOLaneType): List<IOLane> {
        return definition.inputLanes.filter { it.type == type }
    }

    fun getInputLane(laneName: String): IOLane? {
        return definition.inputLanes.find { it.name.equals(laneName, ignoreCase = true) }
    }

    fun getOutputLane(laneName: String): IOLane? {
        return definition.outputLanes.find { it.name.equals(laneName, ignoreCase = true) }
    }

    fun publishOutputEvent(event: KEvent) {
        getOutputLanes(IOLaneType.EVENT)
                .forEach { lane ->
                    if (lane.p2p) sendEvent(getEventBusAddress(lane), event)
                    else publishEvent(getEventBusAddress(lane), event)
                }
    }

    fun publishOutputEvent(event: KEvent, name: String) {
        var lane = getOutputLane(name)
        if (lane == null) log.warn { "Output lane [$name] was not found" }
        if (lane!!.p2p) sendEvent(getEventBusAddress(lane), event)
        else publishEvent(getEventBusAddress(lane), event)
    }

    fun publishEvent(address: String, event: KEvent) {
        val deliveryOptions = DeliveryOptions()
        deliveryOptions.addHeader(ORIGIN_HEADER, id())
        vertx.eventBus().publish(address, event, deliveryOptions)
    }

    fun sendEvent(address: String, event: KEvent) {
        val deliveryOptions = DeliveryOptions()
        deliveryOptions.addHeader(ORIGIN_HEADER, id())
        vertx.eventBus().send(address, event, deliveryOptions)
    }

    fun sendEvent(lane: IOLane, event: KEvent, replyHandler: Handler<AsyncResult<Message<KEvent>>>) {
        val deliveryOptions = DeliveryOptions()
        deliveryOptions.addHeader(ORIGIN_HEADER, id())
        vertx.eventBus().request(getEventBusAddress(lane), event, deliveryOptions, replyHandler)
    }

    fun subscribe(type: IOLaneType, handler: Handler<Message<KEvent>>) {
        val consumers = ArrayList<MessageConsumer<KEvent>>()
        for (ioLane in getInputLanes(type)) {
            val address = getEventBusAddress(ioLane)
            log.trace { "[${definition.instanceName}] listens on ${address}" }
            val messageConsumer = vertx.eventBus().consumer(address, handler)
            consumers.add(messageConsumer)
        }
        verticleConsumers.addAll(consumers)
    }

    fun subscribe(type: IOLaneType, prefix: String, handler: Handler<Message<KEvent>>) {
        val lanes = getInputLanes(type)
        val filtered = lanes.filter { it.name.startsWith(prefix) }

        filtered.forEach {
            val address = getEventBusAddress(it)
            log.debug { "Subscribe to messages from [${address}], lane: [${it.type}/${it.name}]" }
            val messageConsumer = vertx.eventBus().consumer(address, handler)
            verticleConsumers.add(messageConsumer)
        }
    }

    fun subscribe(laneName: String, handler: Handler<Message<KEvent>>) {
        var consumers: List<MessageConsumer<KEvent>> = arrayListOf()
        getInputLane(laneName)?.let {
            val eventBusAddress = getEventBusAddress(it)
            log.debug { "Subscribe to messages from [${eventBusAddress}], lane: [${it.type}/${it.name}]" }
            val messageConsumer = vertx.eventBus().consumer(eventBusAddress, handler)
            consumers += messageConsumer
            verticleConsumers.add(messageConsumer)
        }
    }

    fun eventBusAddressToLaneName(address: String): String {
        return address.split("::").last()
    }

    fun getEventBusAddress(ioLane: IOLane): String {
        return namespace + "::" + ioLane.name
    }

    fun getEventBusAddress(laneName: String): String {
        return namespace + "::" + laneName
    }

    fun id(): String {
        return deploymentID
    }

    override suspend fun stop() {
        super.stop()
        verticleConsumers.forEach { consumer -> if (consumer.isRegistered) consumer.unregister() }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
