package com.apollographql.apollo3.cache.normalized.api

<<<<<<< HEAD
<<<<<<< HEAD
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.readAny
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.use
=======
=======
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
>>>>>>> 5cc55d3b1 (add ApolloDeprecatedSince)
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.cache.normalized.api.internal.JsonRecordSerializer
>>>>>>> 868b3e84e (💧 first drop for a SQLite backend that stores when each field was last updated)

@OptIn(ApolloInternal::class)
@Deprecated("Use JsonRecordSerializer instead")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
object RecordFieldJsonAdapter {
  @Deprecated("Use JsonRecordSerializer instead", ReplaceWith("JsonRecordSerializer.deserialize(json)"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
  fun fromJson(jsonFieldSource: String): Map<String, Any?> {
    return JsonRecordSerializer.deserialize("", jsonFieldSource).fields
  }

<<<<<<< HEAD
  @Suppress("UNCHECKED_CAST")
  fun fromJson(jsonFieldSource: String): Map<String, Any?>? {
    val buffer = Buffer().write(jsonFieldSource.encodeUtf8())
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
=======
  @Deprecated("Use JsonRecordSerializer instead", ReplaceWith("JsonRecordSerializer.serialize(fields)"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
  fun toJson(fields: Map<String, Any?>): String {
    return JsonRecordSerializer.serialize(Record("", fields))
>>>>>>> 868b3e84e (💧 first drop for a SQLite backend that stores when each field was last updated)
  }
}