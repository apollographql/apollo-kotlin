package com.apollographql.apollo3.adapters

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdpaters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

object LocalDateAdapter : Adapter<LocalDate> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): LocalDate {
    return LocalDate.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: LocalDate) {
    writer.value(value.toString())
  }
}

object InstantAdapter : Adapter<Instant> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Instant {
    return Instant.parse(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Instant) {
    writer.value(value.toString())
  }
}