package com.apollographql.apollo3.adapters

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlinx.datetime.Instant
import java.util.Date

object DateResponseAdapter : ResponseAdapter<Date> {
  override fun fromResponse(reader: JsonReader, responseAdapterCache: ResponseAdapterCache): Date {
    return Date(Instant.parse(reader.nextString()!!).toEpochMilliseconds())
  }

  override fun toResponse(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache, value: Date) {
    writer.value(Instant.fromEpochMilliseconds(value.time).toString())
  }
}