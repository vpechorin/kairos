package net.pechorina.kairos.core.verticles.processors

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.automat.Action
import net.pechorina.kairos.automat.Automat
import net.pechorina.kairos.automat.builder.AutomatBuilder
import net.pechorina.kairos.automat.builder.StateConfigurer
import net.pechorina.kairos.automat.builder.TransitionConfigurer
import net.pechorina.kairos.core.Block
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.IOLane
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.mappers.GroovyObjectMapper
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.types.mapNameToContentType
import net.pechorina.kairos.core.verticles.StageCoroutineVerticle
import java.lang.IllegalStateException
import java.util.regex.Pattern

/*
    - section: states
      state: S1
      parent: S0
      entryAction:
      exitAction:
      initialState: false
      finalState: false
    - section: transitions
      source: S1
      target: S2
      action: out_01::true::boolean
      actionAddress: out_01
      actionType: int
      actionPayload: 1
      event: E1
    - section: events
      lane: inputLane01
      event: E1
    - section: events
      lane: inputLane02
      contentType: JSON
      expression: "input?.somekey.textValue()"
*/

class HSMProcessor : StageCoroutineVerticle() {

    lateinit var automat: Automat<String, String>
    lateinit var inputLaneToEventMap: Map<String, EventMapping>
    lateinit var groovyMapper: GroovyObjectMapper

    suspend override fun start() {
        super.start()
        log.debug { "Starting" }
        this.groovyMapper = GroovyObjectMapper()

        val configurer = AutomatBuilder<String, String>()
                .withConfig()
                .enableLogging()

        val stateConfigurer = configurer.configureStates()
        val transitionConfigurer = configurer.configureTransitions()

        definition.opts.section(ConfigKey.States).blocks
                .forEach { processStateBlock(it, stateConfigurer) }

        definition.opts.section(ConfigKey.Transitions)
                .blocks
                .forEach { processTransitionBlock(it, transitionConfigurer) }

        configureInputLaneToEventMappings()

        automat = configurer.build()
        automat.start()

        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
        log.debug { "Startup completed" }
    }

    suspend override fun stop() {
        this.automat?.let { it.stop() }
        super.stop()
    }

    fun configureInputLaneToEventMappings() {
        val eventBlocks = definition.opts.section(ConfigKey.Events).blocks
                .filter { it["lane"] != null }
                .associateBy { it.getString("lane")!! }

        val inputLaneNames = definition.inputLanes
                .filter { it.type == IOLaneType.EVENT }
                .map { it.name }.toSet()

        inputLaneToEventMap = inputLaneNames.associate { laneName ->
            Pair(laneName, toEventMapping(laneName, eventBlocks.get(laneName)))
        }
    }

    fun processStateBlock(stateBlock: Block, stateConfigurer: StateConfigurer<String, String>) {
        val initialState = stateBlock.getBool("initialState", false)
        val finalState = stateBlock.getBool("finalState", false)
        val stateName = stateBlock.getString("state")
                ?: throw RuntimeException("No state found in the block: $stateBlock")
        val parentName = stateBlock.getString("parent")

        val entryActionString = stateBlock.getString("entryAction")
        val entryAction = entryActionString?.let { getAction(entryActionString) }

        val exitActionString = stateBlock.getString("entryAction")
        val exitAction = exitActionString?.let { getAction(exitActionString) }

        stateConfigurer.state(stateName, parentName, initialState, finalState, entryAction, exitAction)
    }

    fun toEventMapping(laneName: String, block: Block?): EventMapping {
        if (block == null) return DirectEventMapping(laneName)

        val expression = block.getString("expression")
        if (expression == null) {
            return DirectEventMapping(laneName, block.getString("event"))
        }

        val inputTypeString = block.getString("contentType", ContentType.TEXT.name)
        val inputType = ContentType.valueOf(inputTypeString)

        val lane = getInputLane(laneName) ?: throw RuntimeException("Can't find lane by name: ${laneName}")

        return GroovyEventMapping(groovyMapper, expression, lane, inputType)
    }

    fun processTransitionBlock(transitionBlock: Block, transitionConfigurer: TransitionConfigurer<String, String>) {
        val sourceState = transitionBlock.getString("source")
        val targetState = transitionBlock.getString("target")
        val eventName = transitionBlock.getString("event")

        val action = transitionBlock.getString("action")
        val actionAddress = transitionBlock.getString("actionAddress")

        if (sourceState == null) throw RuntimeException("Source state is not defined for the config block: ${transitionBlock}")
        if (targetState == null) throw RuntimeException("Target state is not defined for the config block: ${transitionBlock}")
        if (eventName == null) throw RuntimeException("Event is not defined for the config block: ${transitionBlock}")

        val config = transitionConfigurer.withExternal()
                .event(eventName)
                .source(sourceState)
                .target(targetState)

        if (action != null || actionAddress != null) {
            config.action(
                    getActionForTransition(transitionBlock)
            )
        }
    }

    fun getActionForTransition(transitionBlock: Block): Action<String, String> {
        val action = transitionBlock.getString("action")
        if (action != null) return getAction(action)

        val actionAddress = transitionBlock.getString("actionAddress")
        val actionPayload = transitionBlock.getString("actionPayload") ?: throw IllegalStateException("Action payload is not defined")
        val actionType = transitionBlock.getString("actionType")

        return getAction(actionPayload, actionType, actionAddress)
    }

    fun getAction(actionString: String): Action<String, String> {
        val parts = actionString.split(Pattern.compile("::"), 3)

        val actionAddress = if (parts.size > 0) parts[0] else null
        val actionPayload = if (parts.size > 1) parts[1] else throw IllegalStateException("Action payload is not defined")
        val actionType = if (parts.size > 2) parts[2] else null

        return getAction(actionPayload, actionType, actionAddress)
    }

    fun getAction(payload: String, payloadType: String?, outputLane: String?): Action<String, String> {
        if (outputLane == null) {
            return { _, _ -> log.debug { "Empty action" } }
        }

        val contentType = mapNameToContentType(payloadType, ContentType.TEXT)
        log.debug { "action event contentType: $contentType, payload: $payload, type:$payloadType" }
        val outEvent = KEvent(payload).setContentType(contentType)

        return { transition, automat ->
            log.debug { "\nEvent[${transition.event}]\n Transition[${transition.source} --> ${transition.target}]\n - publishing action event to [$outputLane] $outEvent" }
            publishOutputEvent(outEvent, outputLane)
        }
    }

    fun handler(message: Message<KEvent>) {
        launch(vertx.dispatcher()) {
            val inputEvent = message.body()
            val laneName = eventBusAddressToLaneName(message.address())

            val eventMapping = inputLaneToEventMap.getValue(laneName)
            val event = eventMapping.mapToEvent(inputEvent)

            automat.sendEvent(event)
        }
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}

interface EventMapping {
    fun mapToEvent(inputEvent: KEvent): String
}

class DirectEventMapping(val laneName: String, val event: String? = null) : EventMapping {
    override fun mapToEvent(inputEvent: KEvent): String {
        return event ?: laneName
    }
}

class GroovyEventMapping(val groovyMapper: GroovyObjectMapper, val expression: String, val ioLane: IOLane, val inputType: ContentType) : EventMapping {

    override fun mapToEvent(inputEvent: KEvent): String {

        var input = when (inputType) {
            ContentType.JSON -> inputEvent.getPayloadAsJsonObject().map
            else -> inputEvent.getPayload()
        }

        val result = groovyMapper.map(expression, input)

        return result.toString()
    }

    companion object {
        val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()
    }
}