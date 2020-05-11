package net.pechorina.kairos.core.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.avro.Schema
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.util.Base64

object CoersionUtils {

    fun getInteger(value: Any?): Int? {
        return when (value) {
            null -> null
            is Int -> value
            else -> value.toString().toIntOrNull()
        }
    }

    fun getInteger(value: Any?, defaultValue: Int): Int {
        return getInteger(value) ?: defaultValue
    }

    fun getString(value: Any?): String? {
        return when (value) {
            null -> null
            is String -> value
            else -> value.toString()
        }
    }

    fun getString(value: Any?, defaultValue: String): String {
        return getString(value) ?: defaultValue
    }

    fun getCharacter(value: Any?, defaultValue: Char): Char {
        return when (value) {
            null -> defaultValue
            is Char -> value
            else -> value.toString().take(1).single()
        }
    }

    fun getLong(value: Any?): Long? {
        return when (value) {
            null -> null
            is Long -> value
            else -> value.toString().toLongOrNull()
        }
    }

    fun getLong(value: Any?, defaultValue: Long): Long {
        return getLong(value) ?: defaultValue
    }

    fun getDouble(value: Any?): Double? {
        return when (value) {
            null -> null
            is Double -> value
            else -> value.toString().toDoubleOrNull()
        }
    }

    fun getDouble(value: Any?, defaultValue: Double): Double {
        return getDouble(value) ?: defaultValue
    }

    fun getFloat(value: Any?): Float? {
        return when (value) {
            null -> null
            is Float -> value
            else -> value.toString().toFloatOrNull()
        }
    }

    fun getFloat(value: Any?, defaultValue: Float): Float {
        return getFloat(value) ?: defaultValue
    }

    fun getDecimal(value: Any?): BigDecimal? {
        return when (value) {
            null -> null
            is BigDecimal -> value
            is Long -> BigDecimal.valueOf(value)
            is Int -> BigDecimal(value)
            is BigInteger -> BigDecimal(value)
            is Double -> BigDecimal.valueOf(value)
            is String -> BigDecimal(value)
            else -> BigDecimal(value.toString())
        }
    }

    fun getDecimal(value: Any?, defaultValue: BigDecimal): BigDecimal {
        return CoersionUtils.getDecimal(value) ?: defaultValue
    }

    fun getBoolean(value: Any?): Boolean? {
        return when (value) {
            null -> null
            is Boolean -> value
            else -> value.toString().toBoolean()
        }
    }

    fun getBoolean(value: Any?, defaultValue: Boolean): Boolean {
        return getBoolean(value) ?: defaultValue
    }

    fun getBinary(value: Any?): ByteArray? {
        return when (value) {
            null -> null
            else -> Base64.getDecoder().decode(value as String)
        }
    }

    fun getInstant(value: Any?): Instant? {
        if (value == null) return null
        val encoded = value as String
        return Instant.from(ISO_INSTANT.parse(encoded))
    }

    fun getStringObjectMap(value: Any?): Map<String, Any?> {
        if (value == null) return emptyMap()
        return if (value is Map<*, *>) {
            value.map { it.key.toString() to it.value }.toMap()
        } else emptyMap()
    }

    fun getStringMap(value: Any?): Map<String, String> {
        if (value == null) return emptyMap()
        return if (value is Map<*, *>) {
            value.map { it.key.toString() to it.value.toString() }.toMap()
        } else emptyMap()
    }

    fun getList(value: Any?): List<Any> {
        if (value == null) return emptyList()

        if (DataTypeUtils.isStr(value)) {
            val stringValue = DataTypeUtils.TYPES.find { it.type === Schema.Type.STRING }?.converter?.invoke(value) as String
            if (stringValue.contains(",".toRegex())) {
                return stringValue.split(",").map { it.trim() }
            }
            return listOf(stringValue)
        }
        when (value) {
            is List<*> -> return value as List<Any>
            is Array<*> -> return listOf(value as List<Any>)
        }
        return listOf()
    }

    fun getIntList(value: Any?): List<Int> {
        val list = getList(value)
        return list.mapNotNull { CoersionUtils.getInteger(it) }
    }

    fun getLongList(value: Any?): List<Long> {
        val list = getList(value)
        return list.mapNotNull { CoersionUtils.getLong(it) }
    }

    fun getStringList(value: Any?): List<String> {
        val list = getList(value)
        return list.mapNotNull { CoersionUtils.getString(it) }
    }

    fun getDecimalList(value: Any?): List<BigDecimal> {
        val list = getList(value)
        return list.mapNotNull { CoersionUtils.getDecimal(it) }
    }

    fun getJsonList(value: Any?, objectMapper: ObjectMapper): List<String> {
        val list = getList(value)
        return list.mapNotNull { objectMapper.writeValueAsString(it) }
    }

    fun asInt(value: Boolean): Int {
        return if (value) 1 else 0;
    }

    fun asBoolean(value: Int): Boolean {
        return value == 1
    }

}
