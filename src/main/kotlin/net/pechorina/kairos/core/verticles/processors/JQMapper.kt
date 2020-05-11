package net.pechorina.kairos.core.verticles.processors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.types.mapNameToContentType
import net.pechorina.kairos.core.utils.CoersionUtils
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle
import net.thisptr.jackson.jq.JsonQuery
import net.thisptr.jackson.jq.Scope

class JQMapper : StageCoroutineVerticle() {

    lateinit var expression: String
    var outputType: ContentType = ContentType.TEXT
    lateinit var objectMapper: ObjectMapper

    lateinit var jsonQuery: JsonQuery
    lateinit var rootScope: Scope

    suspend override fun start() {
        super.start()
        log.debug { "Starting" }

        expression = definition.opts.section(Section.DEFAULT_SECTION_NAME).block().getString(ConfigKey.Expression.key, "")
        log.debug { "Expression: ${expression}" }

        val outputTypeString = definition.opts.section(Section.DEFAULT_SECTION_NAME).block().getString(ConfigKey.OutputType.key, "TEXT")
        outputType = mapNameToContentType(outputTypeString, ContentType.TEXT)

        log.debug { "OutputType: ${outputType}" }

        objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()

        rootScope = Scope.newEmptyScope()
        rootScope.loadFunctions(rootScope.javaClass.classLoader)

        jsonQuery = JsonQuery.compile(expression)
        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
        log.debug { "Startup completed" }
    }

    fun transformInput(input: String): JsonNode? {
        return objectMapper.readTree(input)
    }

    fun transformOutput(input: List<String>): KEvent {
        return when (outputType) {
            ContentType.INT -> KEvent(CoersionUtils.getInteger(input[0], 0))
            ContentType.LONG -> KEvent(CoersionUtils.getLong(input[0], 0L))
            ContentType.BOOL -> KEvent(CoersionUtils.getBoolean(input[0], false))
            ContentType.TEXT -> KEvent(CoersionUtils.getString(input[0], ""))
            ContentType.JSONA -> KEvent(JsonArray(input))
            ContentType.JSON -> KEvent(JsonObject.mapFrom(input))
            else -> KEvent(CoersionUtils.getString(input, ""))
        }
    }

    fun handler(message: Message<KEvent>) {
        launch(vertx.dispatcher()) {
            val inputEvent = message.body()
            log.debug { "Event: ${inputEvent}" }

            val input = transformInput(inputEvent.getPayloadAsString())
            val childScope = Scope.newChildScope(rootScope)
            val nodeList = jsonQuery.apply(childScope, input)
            val jsonList = CoersionUtils.getJsonList(nodeList, objectMapper)

            val outputEvent = transformOutput(jsonList)
            outputEvent.extendPath(inputEvent, id())
            publishOutputEvent(outputEvent)
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
