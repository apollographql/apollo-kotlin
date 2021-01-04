package com.apollographql.apollo.api.internal.json

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.internal.Throws
import com.apollographql.apollo.api.EnumValue
import okio.IOException
import kotlin.jvm.JvmStatic

object Utils {
  fun writeToJson(value: Any?, jsonWriter: JsonWriter) {
    when (value) {
      null -> jsonWriter.nullValue()

      is Map<*, *> -> {
        jsonWriter.writeObject {
          value.forEach { (key, value) ->
            jsonWriter.name(key.toString())
            writeToJson(value, this)
          }
        }
      }

      is List<*> -> {
        jsonWriter.writeArray {
          value.forEach {
            writeToJson(it, this)
          }
        }
      }

      is Boolean -> jsonWriter.value(value as Boolean?)
      is Number -> jsonWriter.value(value as Number?)
      is EnumValue -> jsonWriter.value(value.rawValue)
      else -> jsonWriter.value(value.toString())
    }
  }

  fun JsonReader.readRecursively(): Any? {
    return when (peek()) {
      JsonReader.Token.NULL -> nextNull()

      JsonReader.Token.BEGIN_OBJECT -> {
        beginObject()

        val result = LinkedHashMap<String, Any?>()
        while (hasNext()) {
          result[nextName()] = readRecursively()
        }

        endObject()
        result
      }

      JsonReader.Token.BEGIN_ARRAY -> {
        beginArray()

        val result = ArrayList<Any?>()
        while (hasNext()) {
          result.add(readRecursively())
        }

        endArray()
        result
      }

      JsonReader.Token.BOOLEAN -> nextBoolean()

      JsonReader.Token.NUMBER -> BigDecimal(nextString()!!)

      JsonReader.Token.LONG -> BigDecimal(nextLong())

      else -> nextString()!!
    }
  }
}
