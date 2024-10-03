package com.apollographql.apollo.cache.normalized.api.internal

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.json.JsonNumber
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.api.RecordValue
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.use

/**
 * An adapter used to serialize and deserialize Record fields. Record object types will be serialized to [CacheKey].
 */
@ApolloInternal
object JsonRecordSerializer {

  fun serialize(record: Record): String  {
    return  toJson(record.fields)
  }

  private fun toJson(fields: Map<String, Any?>): String {
    val buffer = Buffer()
    BufferedSinkJsonWriter(buffer).use { jsonWriter ->
      jsonWriter.beginObject()
      for ((key, value) in fields) {
        jsonWriter.name(key).writeRecordValue(value)
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

  /**
   * returns the [Record] for the given Json
   *
   * @throws if the [Record] cannot be deserialized
   */
  @Suppress("UNCHECKED_CAST")
  fun deserialize(key: String, jsonFieldSource: String): Record {
    val buffer = Buffer().write(jsonFieldSource.encodeUtf8())

    val fields = BufferedSourceJsonReader(buffer)
        .readAny()
        .deserializeCacheKeys() as? Map<String, Any?>

    check (fields != null) {
      "error deserializing: $jsonFieldSource"
    }

    return Record(key, fields)
  }

  @Suppress("UNCHECKED_CAST")
  private fun JsonWriter.writeRecordValue(value: RecordValue) {
    when (value) {
      null -> this.nullValue()
      is String -> this.value(value)
      is Boolean -> this.value(value)
      is Int -> this.value(value)
      is Long -> this.value(value)
      is Double -> this.value(value)
      is JsonNumber -> this.value(value)
      is CacheKey -> this.value(value.serialize())
      is List<*> -> {
        this.beginArray()
        value.forEach { writeRecordValue(it) }
        this.endArray()
      }
      is Map<*, *> -> {
        this.beginObject()
        for (entry in value as Map<String, Any?>) {
          this.name(entry.key).writeRecordValue(entry.value)
        }
        this.endObject()
      }
      else -> error("Unsupported record value type: '$value'")
    }
  }
}
