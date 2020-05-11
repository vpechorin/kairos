package net.pechorina.kairos.core.codecs

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.buffer.Buffer

class JsonCodecHelper<S, R>(val mapper: ObjectMapper) {

    fun encodeToBuffer(buffer: Buffer, obj: S) {
        val json = mapper.writeValueAsString(obj)
        val length = json.toByteArray().size

        // Write data into given buffer
        buffer.appendInt(length)
        buffer.appendString(json)
    }

    fun decodeFromBuffer(pos: Int, buffer: Buffer, clazz: Class<R>): R {
        // My custom message starting from this *position* of buffer
        val _pos = pos

        // Length of JSON
        val length = buffer.getInt(_pos)

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        val start = _pos + 4
        val end = start + length
        val jsonStr = buffer.getString(start, end)

        return mapper.readValue(jsonStr, clazz)
    }
}
