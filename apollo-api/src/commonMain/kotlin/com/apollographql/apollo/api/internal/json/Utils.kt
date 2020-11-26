package com.apollographql.apollo.api.internal.json

import com.apollographql.apollo.api.EnumValue
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
      is EnumValue -> jsonWriter.value(value.rawValue)
      else -> jsonWriter.value(value.toString())
    }
  }
}
