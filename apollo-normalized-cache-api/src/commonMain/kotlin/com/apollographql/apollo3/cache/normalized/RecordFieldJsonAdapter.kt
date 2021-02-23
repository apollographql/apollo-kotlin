package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.internal.json.Utils.readRecursively
import com.apollographql.apollo3.api.json.use
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8

/**
 * An adapter used to serialize and deserialize Record fields. Record object types will be serialized to [CacheReference].
 */
object RecordFieldJsonAdapter {

  fun toJson(fields: Map<String, Any?>): String {
    val buffer = Buffer()
    BufferedSinkJsonWriter(buffer).use { jsonWriter ->
      jsonWriter.serializeNulls = true
      jsonWriter.beginObject()
      for ((key, value) in fields) {
        jsonWriter.name(key).writeJsonValue(value)
      }
      jsonWriter.endObject()
    }
    return buffer.readUtf8()
  }

  private fun Any?.deserializeCacheReferences(): Any? {
    return when (this) {
      is String -> if (CacheReference.canDeserialize(this)) {
        CacheReference.deserialize(this)
      } else {
        this
      }
      is Map<*, *> -> mapValues {
        it.value.deserializeCacheReferences()
      }
      is List<*> -> map { it.deserializeCacheReferences() }
      else -> this
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun fromJson(jsonFieldSource: String): Map<String, Any?>? {
    val buffer = Buffer().write(jsonFieldSource.encodeUtf8())
    return BufferedSourceJsonReader(buffer)
        .readRecursively()
        ?.deserializeCacheReferences() as Map<String, Any?>?
  }

  @Suppress("UNCHECKED_CAST")
  private fun JsonWriter.writeJsonValue(value: Any?) {
    when (value) {
      null -> this.nullValue()
      is String -> this.value(value)
      is Boolean -> this.value(value)
      is Int -> this.value(value)
      is CacheReference -> this.value(value.serialize())
      is List<*> -> {
        this.beginArray()
        value.forEach { writeJsonValue(it) }
        this.endArray()
      }
      is Map<*, *> -> {
        this.beginObject()
        for (entry in value as Map<String, Any?>) {
          this.name(entry.key).writeJsonValue(entry.value)
        }
        this.endObject()
      }
      else -> error("Unsupported record value type: '$value'")
    }
  }
}
