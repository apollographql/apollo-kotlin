package com.apollographql.apollo.tooling

import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import java.time.Instant
import java.time.format.DateTimeFormatter

internal object TimestampAdapter : Adapter<Instant> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Instant {
    return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(reader.nextString()))
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Instant) {
    writer.value(DateTimeFormatter.ISO_INSTANT.format(value))
  }
}
