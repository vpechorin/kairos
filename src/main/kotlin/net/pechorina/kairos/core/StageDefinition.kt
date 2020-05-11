package net.pechorina.kairos.core

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vertx.core.json.JsonObject
import net.pechorina.kairos.core.utils.JsonMapper
import net.pechorina.kairos.core.utils.TemplateEngine
import net.pechorina.kairos.core.utils.YamlMapper
import java.io.BufferedReader
import java.io.File
import java.io.InputStream

class StageDefinition {

    var namespace: String = "default"

    var instanceName: String = "NO_NAME"

    var type: String = "NO_TYPE"

    var deploymentType = DeploymentType.LOCAL

    var worker: Boolean = false
    var highAvailability: Boolean = false
    var instances: Int = 1

    var inputLanes: List<IOLane> = arrayListOf()
    var outputLanes: List<IOLane> = arrayListOf()
    var environment: Map<String, String> = hashMapOf()
    var options: List<Map<String, String>> = arrayListOf()
    var agents: List<AgentConstraint> = arrayListOf()

    @get:JsonIgnore
    val opts: StageOptions by lazy { StageOptions(this.options) }

    fun addInputLane(ioLane: IOLane) {
        this.inputLanes += ioLane
    }

    fun addOutputLane(ioLane: IOLane) {
        this.outputLanes += ioLane
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StageDefinition

        if (namespace != other.namespace) return false
        if (instanceName != other.instanceName) return false
        if (type != other.type) return false
        if (deploymentType != other.deploymentType) return false
        if (worker != other.worker) return false
        if (highAvailability != other.highAvailability) return false
        if (instances != other.instances) return false
        if (inputLanes != other.inputLanes) return false
        if (outputLanes != other.outputLanes) return false
        if (environment != other.environment) return false
        if (options != other.options) return false
        if (agents != other.agents) return false

        return true
    }

    override fun hashCode(): Int {
        var result = namespace.hashCode()
        result = 31 * result + instanceName.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + deploymentType.hashCode()
        result = 31 * result + worker.hashCode()
        result = 31 * result + highAvailability.hashCode()
        result = 31 * result + instances
        result = 31 * result + inputLanes.hashCode()
        result = 31 * result + outputLanes.hashCode()
        result = 31 * result + environment.hashCode()
        result = 31 * result + options.hashCode()
        result = 31 * result + agents.hashCode()
        return result
    }

    override fun toString(): String {
        return "StageDefinition(namespace='$namespace', instanceName='$instanceName', type='$type', deploymentType=$deploymentType, worker=$worker, highAvailability=$highAvailability, instances=$instances, inputLanes=$inputLanes, outputLanes=$outputLanes, environment=$environment, options=$options, agents=$agents)"
    }

    companion object {

        fun preprocessStageDefinition(name: String, body: String, config: JsonObject): String {
            val context = mapOf<String, Any>("config" to config.map)
            return TemplateEngine.process(body, context)
        }

        fun fromYamlInputStream(name: String, inputStream: InputStream?, config: JsonObject): StageDefinition {
            val content = inputStream?.bufferedReader()?.use(BufferedReader::readText)
                    ?: throw RuntimeException("Unable to read stage definition body for $name")
            val body = preprocessStageDefinition(name, content, config)
            return YamlMapper.yamlObjectMapper.readValue(body, StageDefinition::class.java)
        }

        fun fromFile(name: String, file: File, config: JsonObject): StageDefinition {
            val content = file.readText()
            val body = preprocessStageDefinition(name, content, config)
            return YamlMapper.yamlObjectMapper.readValue(body, StageDefinition::class.java)
        }

        fun fromJsonObject(jsonObject: JsonObject): StageDefinition {
            return JsonMapper.jsonObjectMapper.readValue(jsonObject.encode(), StageDefinition::class.java)
        }

        fun fromJson(json: String): StageDefinition {
            return JsonMapper.jsonObjectMapper.readValue(json, StageDefinition::class.java)
        }

        fun fromYaml(yaml: String): StageDefinition {
            val objectMapper = YamlMapper.yamlObjectMapper
            return objectMapper.readValue(yaml, StageDefinition::class.java)
        }

    }
}
