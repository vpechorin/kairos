package net.pechorina.kairos.core

import com.fasterxml.jackson.databind.ObjectMapper
import net.pechorina.kairos.core.utils.CoersionUtils
import java.util.*

data class Block(var options: Map<String, Any> = emptyMap()) {

    private val mapper: ObjectMapper by lazy {
        ObjectMapper()
    }

    fun matches(key: String, value: Any): Boolean {
        return options[key] == value
    }

    operator fun get(key: String): Any? {
        return options[key]
    }

    fun getOrDefault(key: String, defaultValue: Any): Any {
        return options.getOrDefault(key, defaultValue)
    }

    fun getInteger(key: String): Int? {
        return CoersionUtils.getInteger(options[key])
    }

    fun getInteger(key: String, defaultValue: Int): Int {
        return CoersionUtils.getInteger(options[key], defaultValue)
    }

    fun getBool(key: String): Boolean? {
        return CoersionUtils.getBoolean(options[key])
    }

    fun getBool(key: String, defaultValue: Boolean): Boolean {
        return CoersionUtils.getBoolean(options[key], defaultValue)
    }

    fun getString(key: String): String? {
        return CoersionUtils.getString(options[key])
    }

    fun getString(key: String, defaultValue: String): String {
        return CoersionUtils.getString(options[key], defaultValue)
    }

    fun getLong(key: String): Long? {
        return CoersionUtils.getLong(options[key])
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return CoersionUtils.getLong(options[key], defaultValue)
    }

    operator fun get(key: ConfigKey, defaultValue: Any): Any {
        val v = options[key.name]
        when (key.type) {
            ConfigType.BOOL -> return CoersionUtils.getBoolean(v, defaultValue as Boolean)
            ConfigType.CHAR -> return CoersionUtils.getCharacter(v, defaultValue as Char)
            ConfigType.INT -> return CoersionUtils.getInteger(v, defaultValue as Int)
            ConfigType.LONG -> return CoersionUtils.getLong(v, defaultValue as Long)
            ConfigType.NUMBER -> return CoersionUtils.getDouble(v, defaultValue as Double)
            ConfigType.STRING -> return CoersionUtils.getString(v, defaultValue as String)
            ConfigType.DATE -> return CoersionUtils.getString(v, defaultValue as String)
            ConfigType.TEXT -> return CoersionUtils.getString(v, defaultValue as String)
            ConfigType.MAP -> return CoersionUtils.getStringObjectMap(v)
            ConfigType.LIST -> return CoersionUtils.getList(v)
            else -> return v ?: defaultValue
        }
    }

    operator fun get(key: ConfigKey): Any? {
        Objects.requireNonNull(key)
        val v = options[key.key]
        val type = key.type
        when (type) {
            ConfigType.BOOL -> return CoersionUtils.getBoolean(v, type.default as Boolean)
            ConfigType.CHAR -> return CoersionUtils.getCharacter(v, type.default as Char)
            ConfigType.INT -> return CoersionUtils.getInteger(v, type.default as Int)
            ConfigType.LONG -> return CoersionUtils.getLong(v, type.default as Long)
            ConfigType.NUMBER -> return CoersionUtils.getDouble(v, type.default as Double)
            ConfigType.STRING -> return CoersionUtils.getString(v, type.default as String)
            ConfigType.DATE -> return CoersionUtils.getString(v, type.default as String)
            ConfigType.TEXT -> return CoersionUtils.getString(v, type.default as String)
            ConfigType.MAP -> return if (v != null) CoersionUtils.getStringObjectMap(v) else type.default
            ConfigType.LIST -> return if (v != null) CoersionUtils.getList(v) else type.default
            else -> return v ?: type.default
        }
    }

    fun <T> transformTo(clazz: Class<T>): T {
        return this.mapper.convertValue(options, clazz)
    }

    fun asStringMap(): Map<String, String> {
        return options.entries.associateBy({ it.key }, { it.value.toString() })
    }


}
