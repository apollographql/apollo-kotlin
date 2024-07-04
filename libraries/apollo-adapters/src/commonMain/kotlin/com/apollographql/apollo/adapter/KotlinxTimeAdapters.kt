package com.apollographql.apollo.adapter

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * An [Adapter] that converts an ISO 8601 String like "2010-06-01T22:19:44.475Z" to/from
 * a [kotlinx.datetime.Instant]
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
@Deprecated("KotlinxInstantAdapter has new maven coordinates at 'com.apollographql.adapters:apollo-adapters-datetime. See https://go.apollo.dev/ak-moved-artifacts for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
object KotlinxInstantAdapter : Adapter<Instant> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Instant {
    return Instant.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Instant) {
    writer.value(value.toString())
  }
}

/**
 * An [Adapter] that converts an ISO 8601 String without time zone information like "2010-06-01T22:19:44.475" to/from
 * a [kotlinx.datetime.LocalDateTime]
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
@Deprecated("KotlinxLocalDateTimeAdapter has new maven coordinates at 'com.apollographql.adapters:apollo-adapters-datetime. See https://go.apollo.dev/ak-moved-artifacts for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
object KotlinxLocalDateTimeAdapter : Adapter<LocalDateTime> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): LocalDateTime {
    return LocalDateTime.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: LocalDateTime) {
    writer.value(value.toString())
  }
}

/**
 * An [Adapter] that converts an ISO 8601 String like "2010-06-01" to/from
 * a [kotlinx.datetime.LocalDate]
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
@Deprecated("KotlinxLocalDateAdapter has new maven coordinates at 'com.apollographql.adapters:apollo-adapters-datetime. See https://go.apollo.dev/ak-moved-artifacts for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
object KotlinxLocalDateAdapter : Adapter<LocalDate> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): LocalDate {
    return LocalDate.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: LocalDate) {
    writer.value(value.toString())
  }
}

/**
 * An [Adapter] that converts an ISO 8601 String like "14:35:00" to/from
 * a [kotlinx.datetime.LocalDate]
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
@Deprecated("KotlinxLocalTimeAdapter has new maven coordinates at 'com.apollographql.adapters:apollo-adapters-datetime. See https://go.apollo.dev/ak-moved-artifacts for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
object KotlinxLocalTimeAdapter : Adapter<LocalTime> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): LocalTime {
    return LocalTime.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: LocalTime) {
    writer.value(value.toString())
  }
}
