package com.library

import com.apollographql.apollo.api.*
import com.apollographql.apollo.api.json.*

class MyID(val id: String)

class MyIDAdapter() : Adapter<MyID> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): MyID {
    return MyID(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: MyID) {
    writer.value(value.id)
  }
}
