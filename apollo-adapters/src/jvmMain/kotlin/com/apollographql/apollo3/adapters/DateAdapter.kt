package com.apollographql.apollo3.adapters

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdpaters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlinx.datetime.Instant
import java.util.Date

object DateAdapter : Adapter<Date> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Date {
    return Date(Instant.parse(reader.nextString()!!).toEpochMilliseconds())
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Date) {
    writer.value(Instant.fromEpochMilliseconds(value.time).toString())
  }
}