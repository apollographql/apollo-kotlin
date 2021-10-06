package com.apollographql.apollo3.api.internal.json

import com.apollographql.apollo3.api.json.JsonNumber
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

/**
 * Helper methods to read and write generic Json values
 */
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
      is Long -> jsonWriter.value(value)
      is Double -> jsonWriter.value(value)
      is JsonNumber -> jsonWriter.value(value)
      is String -> jsonWriter.value(value)
      else -> error("Cannot write $value to Json")
    }
  }

  fun JsonReader.readRecursivelyFast(): Any? {
    return when (peek()) {
      JsonReader.Token.NULL -> nextNull()

      JsonReader.Token.BEGIN_OBJECT -> {
        beginObject()

        val result = LinkedHashMap<String, Any?>()
        while (hasNext()) {
          result[nextName()] = readRecursivelyFast()
        }
        endObject()
        result
      }

      JsonReader.Token.BEGIN_ARRAY -> {
        beginArray()

        val result = ArrayList<Any?>()
        while (hasNext()) {
          result.add(readRecursivelyFast())
        }

        endArray()
        result
      }

      JsonReader.Token.BOOLEAN -> nextBoolean()

      // Map NUMBER to String
      // Callers can use nextDouble() to convert to Double or write a custom adapter for bigger values
      // This means that for performance reasons, [JsonNumber] will not never be stored int the Map
      JsonReader.Token.NUMBER -> nextString()

      JsonReader.Token.LONG -> nextLong()

      else -> nextString()!!
    }
  }

  /**
   * Reads the reader and maps numbers to the closest representation possible in that order:
   * - Int
   * - Long
   * - Double
   * - JsonNumber
   */
  fun JsonReader.readRecursively(): Any? {
    return when(val token = peek()) {
      JsonReader.Token.NULL -> nextNull()
      JsonReader.Token.BOOLEAN -> nextBoolean()
      JsonReader.Token.LONG, JsonReader.Token.NUMBER -> guessNumber()
      JsonReader.Token.STRING -> nextString()
      JsonReader.Token.BEGIN_OBJECT -> {
        beginObject()
        val result = mutableMapOf<String, Any?>()
        while(hasNext()) {
          result.put(nextName(), readRecursively())
        }
        endObject()
        result
      }
      JsonReader.Token.BEGIN_ARRAY -> {
        beginArray()
        val result = mutableListOf<Any?>()
        while(hasNext()) {
          result.add(readRecursively())
        }
        endArray()
        result
      }
      else -> error("unknown token $token")
    }
  }

  private fun JsonReader.guessNumber(): Any {
    try {
      return nextInt()
    } catch (e: Exception) {
    }
    try {
      return nextLong()
    } catch (e: Exception) {
    }
    try {
      return nextDouble()
    } catch (e: Exception) {
    }
    return nextNumber()
  }
}


internal fun Long.toIntExact(): Int {
  val result = toInt()
  check (result.toLong() == this) {
    "$this cannot be converted to Int"
  }
  return result
}

internal fun Double.toIntExact(): Int {
  val result = toInt()
  check (result.toDouble() == this) {
    "$this cannot be converted to Int"
  }
  return result
}


internal fun Long.toDoubleExact(): Double {
  val result = toDouble()
  check (result.toLong() == this) {
    "$this cannot be converted to Double"
  }
  return result
}

internal fun Double.toLongExact(): Long {
  val result = toLong()
  check (result.toDouble() == this) {
    "$this cannot be converted to Long"
  }
  return result
}