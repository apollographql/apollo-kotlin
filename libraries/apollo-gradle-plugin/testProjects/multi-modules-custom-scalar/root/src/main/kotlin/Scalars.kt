package com.library

import com.apollographql.apollo3.api.*
import com.apollographql.apollo3.api.json.*

class MyID(val id: String)

class MyIDAdapter() : Adapter<MyID> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): MyID {
    return MyID(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: MyID) {
    writer.value(value.id)
  }
}
