package com.example

import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter

class MyStringAdapter : Adapter<String> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: String) {
    writer.value(value)
  }
}
