package net.pechorina.kairos.core.serialization

import io.vertx.core.buffer.Buffer
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.events.KEventSchemaProvider
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.BinaryDecoder
import org.apache.avro.io.BinaryEncoder
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DatumWriter
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.util.Utf8
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

object AvroSerDe {
    val writer: DatumWriter<GenericRecord> = GenericDatumWriter(KEventSchemaProvider.schema)
    val reader: DatumReader<GenericRecord> = GenericDatumReader(KEventSchemaProvider.schema)

    fun serialize(event: KEvent, outputStream: OutputStream) {
        val record: GenericRecord = GenericData.Record(KEventSchemaProvider.schema)
        record.put("id", event.id)
        record.put("timestamp", event.timestamp)
        record.put("path", event.path)
        record.put("headers", event.headers)
        record.put("body", ByteBuffer.wrap(event.body.bytes))

        val encoder: BinaryEncoder = EncoderFactory.get().binaryEncoder(outputStream, null)
        writer.write(record, encoder)
        encoder.flush()
        outputStream.flush()
    }

    fun deserialize(inputStream: InputStream): KEvent? {

        val decoder: BinaryDecoder = DecoderFactory.get().binaryDecoder(inputStream, null)

        var record: GenericRecord = reader.read(null, decoder)

        val body = record.get("body")
        var buffer: Buffer = if (body != null)
            Buffer.buffer((body as ByteBuffer).array())
        else
            Buffer.buffer()

        val id = (record.get("id") as Utf8).toString()
        val timestamp = record.get("timestamp") as Long
        val headers = record.get("headers") as Map<String, String>
        val path = record.get("path") as List<String>

        return KEvent(id, timestamp, path, headers, buffer)
    }
}
