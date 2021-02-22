package com.apollographql.apollo3.api.internal.json

import com.apollographql.apollo3.api.BigDecimal
import com.apollographql.apollo3.api.EnumValue

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

      is Boolean -> jsonWriter.value(value)
      is Int -> jsonWriter.value(value)
      is Double -> jsonWriter.value(value)
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

      JsonReader.Token.NUMBER -> {
        val number = nextString()!!
        // optimize
        if (number.contains('.') || number.contains('e') || number.contains('E')) {
          try {
            number.toDouble()
          } catch (e: Exception) {
            number
          }
        } else {
          try {
            number.toInt()
          } catch (e: Exception) {
            number
          }
        }
      }

      JsonReader.Token.LONG -> {
        val number = nextString()!!
        try {
          number.toInt()
        } catch (e: Exception) {
          number
        }
      }

      else -> nextString()!!
    }
  }
}
