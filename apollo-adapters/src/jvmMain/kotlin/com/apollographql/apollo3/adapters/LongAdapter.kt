package com.apollographql.apollo3.adapters

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdpaters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

object LongAdapter: Adapter<Long>  {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Long {
    return reader.nextString()!!.toLong()
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Long) {
    writer.value(value.toString())
  }
}