package net.pechorina.kairos.core.codecs

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import net.pechorina.kairos.core.StageDefinition

class StageDefinitionMessageCodec(val name: String, mapper: ObjectMapper) : MessageCodec<StageDefinition, StageDefinition> {

    val jsonCodecHelper: JsonCodecHelper<StageDefinition, StageDefinition>

    init {
        this.jsonCodecHelper = JsonCodecHelper(mapper)
    }

    override fun encodeToWire(buffer: Buffer, stageDefinition: StageDefinition) {
        jsonCodecHelper.encodeToBuffer(buffer, stageDefinition)
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): StageDefinition? {
        return jsonCodecHelper.decodeFromBuffer(pos, buffer, StageDefinition::class.java)
    }

    override fun transform(stageDefinition: StageDefinition): StageDefinition {
        return stageDefinition
    }

    override fun name(): String {
        return this.name
    }

    override fun systemCodecID(): Byte {
        return -1
    }
}
