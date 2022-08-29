package com.example

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

class MyLong(val value: Long)


object MyLongAdapter: Adapter<MyLong> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): MyLong {
    return MyLong(reader.nextString()!!.toLong())
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: MyLong) {
    writer.value(value.value.toString())
  }
}
