package net.pechorina.kairos.core.serialization

import java.io.InputStream

interface Deserializer {
    fun deserialize(input: InputStream): Any
    fun deserialize(input: String): Any
}