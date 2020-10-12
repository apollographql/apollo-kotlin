package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.internal.Throws
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.use
import com.apollographql.apollo.cache.normalized.internal.CacheJsonStreamReader
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import okio.IOException
import kotlin.jvm.JvmStatic

/**
 * An adapter used to serialize and deserialize Record fields. Record object types will be serialized to
 * [CacheReference].
 */
class RecordFieldJsonAdapter {

  fun toJson(fields: Map<String, Any?>): String {
    val buffer = Buffer()
    return JsonWriter.of(buffer).use { jsonWriter ->
      jsonWriter.serializeNulls = true
      try {
        jsonWriter.beginObject()
        for ((key, value) in fields) {
          jsonWriter.name(key)
          writeJsonValue(value, jsonWriter)
        }
        jsonWriter.endObject()
        jsonWriter.close()
        buffer.readUtf8()
      } catch (e: IOException) {
        // should never happen as we are working with mem buffer
        throw RuntimeException(e)
      }
    }
  }

  @Throws(IOException::class)
  private fun fromBufferSource(bufferedFieldSource: BufferedSource): Map<String, Any?>? {
    val cacheJsonStreamReader = CacheJsonStreamReader(BufferedSourceJsonReader(bufferedFieldSource))
    return cacheJsonStreamReader.toMap()
  }

  @Throws(IOException::class)
  fun from(jsonFieldSource: String): Map<String, Any?>? {
    return fromBufferSource(Buffer().write(jsonFieldSource.encodeUtf8()))
  }

  companion object {

    @Suppress("UNCHECKED_CAST")
    @Throws(IOException::class)
    private fun writeJsonValue(value: Any?, jsonWriter: JsonWriter) {
      when (value) {
        null -> jsonWriter.nullValue()
        is String -> jsonWriter.value(value)
        is Boolean -> jsonWriter.value(value)
        is Number -> jsonWriter.value(value)
        is CacheReference -> jsonWriter.value(value.serialize())
        is List<*> -> {
          jsonWriter.beginArray()
          value.forEach { writeJsonValue(it, jsonWriter) }
          jsonWriter.endArray()
        }
        is Map<*, *> -> {
          jsonWriter.beginObject()
          for (entry in value as Map<String, Any?>) {
            jsonWriter.name(entry.key)
            writeJsonValue(entry.value, jsonWriter)
          }
          jsonWriter.endObject()
        }
        else -> error("Unsupported record value type: ${value::class.qualifiedName}")
      }
    }
  }
}
