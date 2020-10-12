package com.apollographql.apollo.api.internal.json

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.internal.Throws
import okio.IOException
import kotlin.jvm.JvmStatic

object Utils {

  @JvmStatic
  @Throws(IOException::class)
  fun writeToJson(value: Any?, jsonWriter: JsonWriter) {
    when (value) {
      null -> jsonWriter.nullValue()

      is Map<*, *> -> {
        jsonWriter.beginObject().apply {
          value.forEach { (key, value) ->
            jsonWriter.name(key.toString())
            writeToJson(value, this)
          }
        }.endObject()
      }

      is List<*> -> {
        jsonWriter.beginArray().apply {
          value.forEach {
            writeToJson(it, this)
          }
        }.endArray()
      }

      is Boolean -> jsonWriter.value(value as Boolean?)
      is Number -> jsonWriter.value(value as Number?)
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

      else -> nextString()!!
    }
  }
}
