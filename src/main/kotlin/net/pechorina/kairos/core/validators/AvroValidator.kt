package net.pechorina.kairos.core.validators


import net.pechorina.kairos.core.http.ValidationError
import org.apache.avro.AvroTypeException
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.io.DecoderFactory
import java.io.ByteArrayInputStream
import java.io.DataInputStream

object AvroValidator {

    fun validateJson(json: String, schema: Schema): ValidationError? {
        try {
            ByteArrayInputStream(json.toByteArray()).use { input ->
                DataInputStream(input).use { din ->
                    val decoder = DecoderFactory.get().jsonDecoder(schema, din)
                    val reader = GenericDatumReader<Void>(schema)
                    reader.read(null, decoder)
                    return null
                }
            }
        } catch (e: AvroTypeException) {
            return return asError(e)
        }

    }

    private fun asError(e: AvroTypeException): ValidationError {
        return ValidationError(message = e.message)
    }
}
