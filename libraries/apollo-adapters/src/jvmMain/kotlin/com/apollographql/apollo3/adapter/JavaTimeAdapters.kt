package com.apollographql.apollo3.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


/**
 * An [Adapter] that converts an ISO 8601 String to/from a [java.time.Instant]
 * When writing, it discards the offset information.
 *
 * Examples:
 * - "2010-06-01T22:19:44.475Z"
 * - "2010-06-01T23:19:44.475+01:00"
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
object JavaInstantAdapter : Adapter<Instant> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Instant {
    // Instant.parse chokes on offset (kotlinx.datetime.Instant doesn't)
    return OffsetDateTime.parse(reader.nextString()!!).toInstant()
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Instant) {
    writer.value(value.toString())
  }
}

/**
 * An [Adapter] that converts a date to/from [java.time.LocalDate]
 *
 * Examples:
 * - "2010-06-01"
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
object JavaLocalDateAdapter : Adapter<LocalDate> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): LocalDate {
    return LocalDate.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: LocalDate) {
    writer.value(value.toString())
  }
}

/**
 * An [Adapter] that converts a date and time to/from [java.time.LocalDateTime]
 *
 * Examples:
 * - "2010-06-01T22:19:44.475"
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
object JavaLocalDateTimeAdapter : Adapter<LocalDateTime> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): LocalDateTime {
    return LocalDateTime.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: LocalDateTime) {
    writer.value(value.toString())
  }
}

/**
 * An [Adapter] that converts a date and time to/from [java.time.OffsetDateTime]
 *
 * Examples:
 * - "2010-06-01T22:19:44.475+01:00"
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
object JavaOffsetDateTimeAdapter : Adapter<OffsetDateTime> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): OffsetDateTime {
    return OffsetDateTime.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: OffsetDateTime) {
    writer.value(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }
}

/**
 * An [Adapter] that converts a time to/from [java.time.LocalTime]
 *
 * Examples:
 * - "14:35:00"
 *
 * It requires Android Gradle plugin 4.0 or newer and [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
 */
object JavaLocalTimeAdapter : Adapter<LocalTime> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): LocalTime {
    return LocalTime.parse(reader.nextString())
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: LocalTime) {
    writer.value(value.toString())
  }
}
