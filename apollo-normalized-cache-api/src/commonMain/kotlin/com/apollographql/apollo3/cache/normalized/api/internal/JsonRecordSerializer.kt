package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.Record
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.use

/**
 * An adapter used to serialize and deserialize Record fields. Record object types will be serialized to [CacheKey].
 */
@ApolloInternal
object JsonRecordSerializer {
  private const val KEY_ARGUMENTS = "__arguments"
  private const val KEY_METADATA = "__metadata"

  fun serialize(record: Record): String {
    return toJson(record)
  }

  private fun toJson(record: Record): String {
    val buffer = Buffer()
    BufferedSinkJsonWriter(buffer).use { jsonWriter ->
      jsonWriter.beginObject()
      for ((key, value) in record.fields) {
        jsonWriter.name(key).writeJsonValue(value)
      }
      jsonWriter.name(KEY_ARGUMENTS).writeObject {
        for ((key, value) in record.arguments) {
          jsonWriter.name(key).writeJsonValue(value)
        }
      }
      jsonWriter.name(KEY_METADATA).writeObject {
        for ((key, value) in record.metadata) {
          jsonWriter.name(key).writeJsonValue(value)
        }
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

    val allFields = BufferedSourceJsonReader(buffer).readAny() as Map<String, Any?>
    val fields = allFields
        .filterKeys { it != KEY_ARGUMENTS && it != KEY_METADATA }
        .deserializeCacheKeys() as? Map<String, Any?>

    check(fields != null) {
      "error deserializing: $jsonFieldSource"
    }

    return Record(
        key = key,
        fields = fields,
        mutationId = null,
        date = emptyMap(),
        arguments = allFields[KEY_ARGUMENTS] as Map<String, Any?>,
        metadata = allFields[KEY_METADATA] as Map<String, Any?>)
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
