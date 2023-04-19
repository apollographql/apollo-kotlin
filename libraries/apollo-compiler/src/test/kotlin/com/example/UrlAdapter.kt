package com.example

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

object UrlAdapter : Adapter<String> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: ScalarAdapters): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: ScalarAdapters, value: String) {
    writer.value(value)
  }
}
