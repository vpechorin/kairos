package net.pechorina.kairos.core.serialization

import java.io.OutputStream

interface Serializer {
    fun serialize(obj: Any, outputStream: OutputStream)
    fun serialize(obj: Any): String
}