package net.pechorina.kairos.core.codecs

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.serialization.AvroSerDe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class KEventMessageCodec(val name: String) : MessageCodec<KEvent, KEvent> {

    override fun encodeToWire(buffer: Buffer, event: KEvent) {
        val outputStream = ByteArrayOutputStream();
        AvroSerDe.serialize(event, outputStream)
        val bytes = outputStream.toByteArray()
        buffer.appendInt(bytes.size)
        buffer.appendBytes(bytes)
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): KEvent? {
        val length = buffer.getInt(pos)
        val bytes = buffer.getBytes(pos + 4, pos + length + 4)
        return AvroSerDe.deserialize(ByteArrayInputStream(bytes))
    }

    override fun transform(event: KEvent): KEvent {
        return event
    }

    override fun name(): String {
        return this.name
    }

    override fun systemCodecID(): Byte {
        return -1
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
