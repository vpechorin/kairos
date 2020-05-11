package net.pechorina.kairos.serialization

import com.google.common.base.Stopwatch
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.codecs.JsonCodecHelper
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.serialization.AvroSerDe
import net.pechorina.kairos.core.utils.JsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class AvroSerDeUnitTest {

    val codecHelper = JsonCodecHelper<KEvent, KEvent>(JsonMapper.jsonObjectMapper)

    @Test
    fun serializeTest() {
        var payload = json {
            obj {
                "username" to "John"
                "password" to "secret"
            }
        }
        val origin = UUID.randomUUID().toString();
        val id = UUID.randomUUID().toString();
        val event = KEvent(payload).addPath(origin).addPath(id)

        var t1 = Stopwatch.createStarted()
            val outputStream = ByteArrayOutputStream();
            AvroSerDe.serialize(event, outputStream)
            //val ba = outputStream.toByteArray()
            val avroBuffer = Buffer.buffer(outputStream.toByteArray())
        t1.stop()

        //assertThat(ba.size).isEqualTo(153)

        var t2 = Stopwatch.createStarted()
        for (j in 1..10) {
            val jsonBuffer = Buffer.buffer()
            codecHelper.encodeToBuffer(jsonBuffer, event)
            log.debug { "$j" }
        }
        t2.stop()

        log.debug { "Avro: ${t1.elapsed(TimeUnit.MILLISECONDS)}, Json: ${t2.elapsed(TimeUnit.MILLISECONDS)}" }

        //assertThat(jsonBuffer.length()).isEqualTo(153)


//        val inp = ByteArrayInputStream(ba)
//        val out = AvroSerDe.deserialize(inp)
//        assertThat(out?.getPayloadAsString()).contains("test")
//        assertThat(out?.id).isEqualTo(event.id)
//        assertThat(out?.timestamp).isEqualTo(event.timestamp)
    }

//    @Test
//    fun testDeserialize() {
//        val def = StageDefinition.fromYaml(yaml)
//        assertThat(def).isNotNull
//        assertThat(def.namespace).isEqualTo("core")
//        assertThat(def.instanceName).isEqualTo("EventLogSink_01")
//        assertThat(def.type).isEqualTo("net.pechorina.kairos.core.verticles.sinks.EventLogSink")
//        assertThat(def.inputLanes).extracting("type").containsOnly(IOLaneType.EVENT)
//    }


    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}