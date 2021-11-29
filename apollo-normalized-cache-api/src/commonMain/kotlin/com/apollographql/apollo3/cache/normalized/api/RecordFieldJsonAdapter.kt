package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.readAny
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.use

/**
 * An adapter used to serialize and deserialize Record fields. Record object types will be serialized to [CacheKey].
 */
object RecordFieldJsonAdapter {

  fun toJson(fields: Map<String, Any?>): String {
    val buffer = Buffer()
    BufferedSinkJsonWriter(buffer).use { jsonWriter ->
      jsonWriter.beginObject()
      for ((key, value) in fields) {
        jsonWriter.name(key).writeJsonValue(value)
      }
      jsonWriter.endObject()
    }
    return buffer.readUtf8()
  }

  private fun Any?.deserializeCacheKeys(): Any? {
    return when (this) {
      is String -> if (CacheKey.canDeserialize(this)) {
        CacheKey.deserialize(this)
      } else {
        this
      }
      is Map<*, *> -> mapValues {
        it.value.deserializeCacheKeys()
      }
      is List<*> -> map { it.deserializeCacheKeys() }
      else -> this
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun fromJson(jsonFieldSource: String): Map<String, Any?>? {
    val buffer = Buffer().write(jsonFieldSource.encodeUtf8())
    @OptIn(ApolloInternal::class)
    return BufferedSourceJsonReader(buffer)
        .readAny()
        ?.deserializeCacheKeys() as Map<String, Any?>?
  }

  @Suppress("UNCHECKED_CAST")
  private fun JsonWriter.writeJsonValue(value: Any?) {
    when (value) {
      null -> this.nullValue()
      is String -> this.value(value)
      is Boolean -> this.value(value)
      is Int -> this.value(value)
      is Long -> this.value(value)
      is Double -> this.value(value)
      is CacheKey -> this.value(value.serialize())
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
