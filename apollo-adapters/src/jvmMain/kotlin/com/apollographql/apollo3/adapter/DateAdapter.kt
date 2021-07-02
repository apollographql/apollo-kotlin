package com.apollographql.apollo3.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlinx.datetime.Instant
import java.util.Date

/**
 * An [Adapter] that converts an ISO 8601 String like "2010-06-01T22:19:44.475Z" to/from
 * a java [Date]
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
object DateAdapter : Adapter<Date> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Date {
    return Date(Instant.parse(reader.nextString()!!).toEpochMilliseconds())
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Date) {
    writer.value(Instant.fromEpochMilliseconds(value.time).toString())
  }
}