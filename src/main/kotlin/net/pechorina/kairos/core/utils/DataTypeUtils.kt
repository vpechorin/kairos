package net.pechorina.kairos.core.utils

import org.apache.avro.Schema
import org.apache.commons.lang3.math.NumberUtils
import java.util.*

object DataTypeUtils {
    val DEFAULT_TYPE = Schema.Type.STRING
    val DEFAULT_CONVERTER: (input: Any?) -> Any? = { o: Any? -> o?.toString() }
    /*
    Avro types:
     RECORD, ENUM, ARRAY, MAP, UNION, FIXED, STRING, BYTES,
          INT, LONG, FLOAT, DOUBLE, BOOLEAN, NULL
     */
    val TYPES = listOf(
            TT(Schema.Type.NULL, { isNull(it) }, { it -> it }),
            TT(Schema.Type.ARRAY, { isArray(it) }, { it -> CoersionUtils.getList(it) }),
            TT(Schema.Type.MAP, { isMap(it) }, { CoersionUtils.getStringObjectMap(it) }),
            TT(Schema.Type.BOOLEAN, { isBoolean(it) }, { CoersionUtils.getBoolean(it) }),
            TT(Schema.Type.BYTES, { isBinary(it) }, { CoersionUtils.getBinary(it) }),
            TT(Schema.Type.INT, { isInt(it) }, { CoersionUtils.getInteger(it) }),
            TT(Schema.Type.LONG, { isLong(it) }, { CoersionUtils.getLong(it) }),
            TT(Schema.Type.FLOAT, { isFloat(it) }, { CoersionUtils.getFloat(it) }),
            TT(Schema.Type.DOUBLE, { isDouble(it) }, { CoersionUtils.getDouble(it) }),
            TT(Schema.Type.STRING, { isStr(it) }, { CoersionUtils.getString(it) })
    )

    fun getTypeAndConvert(value: Any): Pair<Schema.Type, Any?> {

        for (type in TYPES) {
            if (type.predicate.invoke(value)) {
                return Pair(type.type, type.converter.invoke(value))
            }
        }
        return Pair(DEFAULT_TYPE, DEFAULT_CONVERTER.invoke(value))
    }

    fun isNull(value: Any?): Boolean {
        return value == null
    }

    fun isStr(value: Any?): Boolean {
        return when (value) {
            null -> false
            is String -> true
            is CharArray -> true
            else -> false
        }
    }

    fun isInt(value: Any?): Boolean {
        if (value == null) return false

        if (value is Int) {
            return true
        }

        val intString = value.toString()

        if (NumberUtils.isParsable(intString)) {
            try {
                Integer.parseInt(intString)
                return true
            } catch (e: NumberFormatException) {
                // ignore
            }

        }

        return false
    }

    fun isLong(value: Any?): Boolean {
        if (value == null) return false

        if (value is Long) {
            return true
        }

        val longString = value.toString()

        if (NumberUtils.isParsable(longString)) {
            try {
                java.lang.Long.parseLong(longString)
                return true
            } catch (e: NumberFormatException) {
                // ignore
            }

        }

        return false
    }

    fun isDouble(value: Any?): Boolean {
        if (value == null) return false

        if (value is Double) {
            return true
        }

        val doubleString = value.toString()

        if (NumberUtils.isParsable(doubleString)) {
            try {
                java.lang.Double.parseDouble(doubleString)
                return true
            } catch (e: NumberFormatException) {
                // ignore
            }

        }

        return false
    }

    fun isFloat(value: Any?): Boolean {
        if (value == null) return false

        if (value is Float) {
            return true
        }

        val floatString = value.toString()

        if (NumberUtils.isParsable(floatString)) {
            try {
                java.lang.Double.parseDouble(floatString)
                return true
            } catch (e: NumberFormatException) {
                // ignore
            }

        }

        return false
    }

    fun isBoolean(value: Any?): Boolean {
        if (value == null) return false

        if (value is Boolean) {
            return true
        }
        val stringValue = value.toString()
        return stringValue.equals("true", ignoreCase = true) || stringValue.equals("false", ignoreCase = true)
    }

    fun isBinary(value: Any?): Boolean {
        return if (value == null) false else value is ByteArray
    }

    fun isArray(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Array<*> -> true
            is ArrayList<*> -> true
            is List<*> -> true
            else -> false
        }
    }

    fun isMap(value: Any?): Boolean {
        return when (value) {
            null -> false
            is LinkedHashMap<*, *> -> true
            is HashMap<*, *> -> true
            is TreeMap<*, *> -> true
            is Map<*, *> -> true
            else -> false
        }
    }

    data class TT(val type: Schema.Type, val predicate: (input: Any?) -> Boolean, val converter: (input: Any?) -> Any?)
}
