package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.dispatcher
import jetbrains.exodus.bindings.StringBinding
import jetbrains.exodus.env.Environment
import jetbrains.exodus.env.EnvironmentConfig
import jetbrains.exodus.env.Environments
import jetbrains.exodus.env.StoreConfig
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.Section
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.mappers.GroovyObjectMapper
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle

class KeyValueStore : StageCoroutineVerticle() {

    lateinit var storePath: String
    lateinit var storeName: String
    lateinit var environment: Environment
    lateinit var inputType: ContentType
    val groovyMapper: GroovyObjectMapper = GroovyObjectMapper()
    var keyTransformation: String? = null
    lateinit var valueTransformation: String

    suspend override fun start() {
        super.start()

        this.storePath = definition.opts
                .section(Section.DEFAULT_SECTION_NAME)
                .block()
                .getString(ConfigKey.Path.key, "/tmp/" + System.currentTimeMillis())

        this.storeName = definition.opts
                .section(Section.DEFAULT_SECTION_NAME)
                .block()
                .getString(ConfigKey.StoreName.key, "public")

        val config = EnvironmentConfig()
                .setGcEnabled(true)
                .setGcRunPeriod(180000)

        this.environment = Environments.newInstance(this.storePath, config)

        subscribe(IOLaneType.INTERACTIVE, "GET", Handler { this.handlerGET(it) })
        subscribe(IOLaneType.EVENT, "PUT", Handler { this.handlerPUT(it) })
        subscribe(IOLaneType.EVENT, "DELETE", Handler { this.handlerDELETE(it) })
    }

    override suspend fun stop() {
        super.stop()

        this.environment.close()
    }

    private fun handlerGET(message: Message<KEvent>) {
        val event = message.body()
        val key = event.getPayloadAsString()
        log.debug { "GET handler, key:[${key}], event: ${event.id}" }

        if (key.isNullOrEmpty()) {
            val replyEvent = KEvent()
                    .setStatus(Status.NOT_FOUND)
            message.reply(replyEvent)
            return
        }

        launch(vertx.dispatcher()) {
            val value = get(key)

            val outputEvent = if (value != null) {
                KEvent(json { obj(key to value) })
            } else {
                KEvent().setStatus(Status.NOT_FOUND)
            }
            outputEvent.extendPath(event, id())
            message.reply(outputEvent)
        }
    }

    fun get(key: String): String? {
        log.debug("GET key: [${key}]")
        var value: String? = null
        this.environment.executeInTransaction { transaction ->
            val store = environment.openStore(this.storeName, StoreConfig.WITHOUT_DUPLICATES, transaction)
            value = store.get(transaction, StringBinding.stringToEntry(key))?.let { StringBinding.entryToString(it) }
        }
        log.debug { "Retrieved: [${key}]: [${value}]" }
        return value
    }

    fun put(key: String, value: String) {
        log.debug("PUT key:[${key}] value:[${value}]")
        environment.executeInTransaction { transaction ->
            val store = environment.openStore(this.storeName, StoreConfig.WITHOUT_DUPLICATES, transaction)
            store.put(transaction, StringBinding.stringToEntry(key), StringBinding.stringToEntry(value))
        }
    }

    fun delete(key: String) {
        log.debug("DELETE key:[${key}]")
        environment.executeInTransaction { transaction ->
            val store = environment.openStore(this.storeName, StoreConfig.WITHOUT_DUPLICATES, transaction)
            store.delete(transaction, StringBinding.stringToEntry(key))
        }
    }

    private fun handlerPUT(message: Message<KEvent>) {
        val event = message.body()
        val json = event.getPayloadAsJsonObject()
        if (json.isEmpty) {
            message.reply(KEvent().setStatus(Status.BAD_REQUEST))
            return
        }
        val map = json.map


        launch(vertx.dispatcher()) {
            map.entries.forEach {
                val key = it.key ?: "__NULL"
                val value = it.value
                if (value == null) {
                    log.debug("PUT handler - Delete - key:${key}, event: ${event.id}")
                    delete(key)
                } else {
                    log.debug("PUT handler - Put - key:${key}, value:${value}, event: ${event.id}")
                    put(key, "$value")
                }
            }
        }
    }

    private fun handlerDELETE(message: Message<KEvent>) {
        val event = message.body()
        val key = event.getPayloadAsString()
        if (key.isNullOrEmpty()) {
            val replyEvent = KEvent()
                    .setStatus(Status.NOT_FOUND)
            message.reply(replyEvent)
            return
        }

        log.debug("DELETE handler key:${key}, event: ${event.id}")

        launch(vertx.dispatcher()) {
            delete(key)
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
