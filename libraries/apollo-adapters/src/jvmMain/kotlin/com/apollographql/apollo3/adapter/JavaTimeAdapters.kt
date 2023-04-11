package com.apollographql.apollo3.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ScalarAdapter
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
object JavaInstantAdapter : ScalarAdapter<Instant> {
  override fun fromJson(reader: JsonReader): Instant {
    // Instant.parse chokes on offset (kotlinx.datetime.Instant doesn't)
    return OffsetDateTime.parse(reader.nextString()!!).toInstant()
  }

  override fun toJson(writer: JsonWriter, value: Instant) {
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
object JavaLocalDateAdapter : ScalarAdapter<LocalDate> {
  override fun fromJson(reader: JsonReader): LocalDate {
    return LocalDate.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, value: LocalDate) {
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
object JavaLocalDateTimeAdapter : ScalarAdapter<LocalDateTime> {
  override fun fromJson(reader: JsonReader): LocalDateTime {
    return LocalDateTime.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, value: LocalDateTime) {
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
object JavaOffsetDateTimeAdapter : ScalarAdapter<OffsetDateTime> {
  override fun fromJson(reader: JsonReader): OffsetDateTime {
    return OffsetDateTime.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, value: OffsetDateTime) {
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
object JavaLocalTimeAdapter : ScalarAdapter<LocalTime> {
  override fun fromJson(reader: JsonReader): LocalTime {
    return LocalTime.parse(reader.nextString())
  }

  override fun toJson(writer: JsonWriter, value: LocalTime) {
    writer.value(value.toString())
  }
}
