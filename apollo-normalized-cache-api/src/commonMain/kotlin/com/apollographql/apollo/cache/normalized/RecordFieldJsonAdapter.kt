package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.Utils.readRecursively
import com.apollographql.apollo.api.internal.json.use
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8

/**
 * An adapter used to serialize and deserialize Record fields. Record object types will be serialized to [CacheReference].
 */
object RecordFieldJsonAdapter {

  fun toJson(fields: Map<String, Any?>): String {
    val buffer = Buffer()
    JsonWriter.of(buffer)
        .apply { serializeNulls = true }
        .use { jsonWriter -> Utils.writeToJson(fields, jsonWriter) }
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
}
