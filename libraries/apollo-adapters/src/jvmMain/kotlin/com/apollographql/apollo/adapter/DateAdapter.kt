package com.apollographql.apollo.adapter

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
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
@Deprecated("DateAdapter has new maven coordinates at 'com.apollographql.adapters:apollo-adapters-core. See https://go.apollo.dev/ak-moved-artifacts for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
object DateAdapter : Adapter<Date> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Date {
    return Date(OffsetDateTime.parse(reader.nextString()!!).toInstant().toEpochMilli())
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Date) {
    writer.value(Instant.ofEpochMilli(value.time).toString())
  }
}