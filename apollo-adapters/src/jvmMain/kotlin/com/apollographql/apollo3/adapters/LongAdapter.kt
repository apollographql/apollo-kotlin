package com.apollographql.apollo3.adapters

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

object LongAdapter: Adapter<Long>  {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Long {
    return reader.nextString()!!.toLong()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Long) {
    writer.value(value.toString())
  }
}