package com.library

import com.apollographql.apollo3.api.*
import com.apollographql.apollo3.api.json.*

class MyID(val id: String)

class MyIDAdapter() : Adapter<MyID> {
  override fun fromJson(reader: JsonReader): MyID {
    return MyID(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, value: MyID) {
    writer.value(value.id)
  }
}
