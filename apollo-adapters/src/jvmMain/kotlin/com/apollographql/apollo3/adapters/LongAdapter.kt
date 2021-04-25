package com.apollographql.apollo3.adapters

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

object LongAdapter: ResponseAdapter<Long>  {
  override fun fromResponse(reader: JsonReader, responseAdapterCache: ResponseAdapterCache): Long {
    return reader.nextString()!!.toLong()
  }

  override fun toResponse(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache, value: Long) {
    writer.value(value.toString())
  }
}