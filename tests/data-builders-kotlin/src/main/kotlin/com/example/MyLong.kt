package com.example

import com.apollographql.apollo3.api.ScalarAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

class MyLong(val value: Long)


object MyLongAdapter : ScalarAdapter<MyLong> {
  override fun fromJson(reader: JsonReader): MyLong {
    return MyLong(reader.nextString()!!.toLong())
  }

  override fun toJson(writer: JsonWriter, value: MyLong) {
    writer.value(value.value.toString())
  }
}
