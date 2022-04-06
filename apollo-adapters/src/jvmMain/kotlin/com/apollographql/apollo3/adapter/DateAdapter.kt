package com.apollographql.apollo3.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Date

/**
 * An [Adapter] that converts an ISO 8601 String to/from a [java.util.Date]
 * When writing, it discards the offset information.
 *
 * Examples:
 * - "2010-06-01T22:19:44.475Z"
 * - "2010-06-01T23:19:44.475+01:00"
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
object DateAdapter : Adapter<Date> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Date {
    return Date(OffsetDateTime.parse(reader.nextString()!!).toInstant().toEpochMilli())
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Date) {
    writer.value(Instant.ofEpochMilli(value.time).toString())
  }
}