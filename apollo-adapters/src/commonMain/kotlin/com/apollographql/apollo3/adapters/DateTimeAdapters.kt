package com.apollographql.apollo3.adapters

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

object LocalDateResponseAdapter : ResponseAdapter<LocalDate> {
  override fun fromResponse(reader: JsonReader, responseAdapterCache: ResponseAdapterCache): LocalDate {
    return LocalDate.parse(reader.nextString()!!)
  }

  override fun toResponse(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache, value: LocalDate) {
    writer.value(value.toString())
  }
}

object InstantResponseAdapter : ResponseAdapter<Instant> {
  override fun fromResponse(reader: JsonReader, responseAdapterCache: ResponseAdapterCache): Instant {
    return Instant.parse(reader.nextString()!!)
  }

  override fun toResponse(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache, value: Instant) {
    writer.value(value.toString())
  }
}