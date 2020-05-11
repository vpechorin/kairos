package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
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
import net.pechorina.kairos.core.types.mapNameToContentType
import net.pechorina.kairos.core.utils.CoersionUtils
import net.pechorina.kairos.core.utils.JsonMapper
import net.pechorina.kairos.core.utils.XmlObjectMapper
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle

class GroovyMapper : StageCoroutineVerticle() {

    var outputType: ContentType = ContentType.TEXT
    lateinit var groovyMapper: GroovyObjectMapper
    lateinit var groovyExpression: String

    suspend override fun start() {
        super.start()

        this.groovyExpression = definition.opts.section(Section.DEFAULT_SECTION_NAME).block().getString(ConfigKey.Expression.key, "")
        this.groovyMapper = GroovyObjectMapper()

        val resultTypeString = definition.opts.section(Section.DEFAULT_SECTION_NAME).block().getString(ConfigKey.OutputType.key, "TEXT")
        outputType = mapNameToContentType(resultTypeString, ContentType.TEXT)

        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
    }

    fun transformInput(input: KEvent): Any? {

        return when (input.getContentType()) {
            null -> null
            ContentType.JSON -> JsonMapper.jsonObjectMapper.readTree(input.getPayloadAsString())
            ContentType.JSONA -> JsonMapper.jsonObjectMapper.readTree(input.getPayloadAsString())
            ContentType.XML -> XmlObjectMapper.xmlObjectMapper.readTree(input.getPayloadAsString())
            else -> input
        }
    }

    fun transformOutput(input: Any?): KEvent {
        if (input == null) return KEvent().setStatus(Status.NO_CONTENT)

        return when (outputType) {
            ContentType.INT -> KEvent(CoersionUtils.getInteger(input, 0))
            ContentType.LONG -> KEvent(CoersionUtils.getLong(input, 0L))
            ContentType.BOOL -> KEvent(CoersionUtils.getBoolean(input, false))
            ContentType.TEXT -> KEvent(CoersionUtils.getString(input, ""))
            ContentType.JSON -> KEvent(JsonObject.mapFrom(input))
            ContentType.JSONA -> KEvent(JsonArray(input as List<*>))
            else -> KEvent(CoersionUtils.getString(input, ""))
        }
    }

    fun handler(message: Message<KEvent>) {
        launch(vertx.dispatcher()) {

            val inputEvent = message.body()

            val input = transformInput(message.body())

            val transformed = groovyMapper.map(groovyExpression, input)

            val outputEvent = transformOutput(transformed)
                    .extendPath(inputEvent, id())
            publishOutputEvent(outputEvent)
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
