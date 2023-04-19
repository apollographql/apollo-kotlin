package com.example

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

object UrlAdapter : Adapter<String> {
  override fun fromJson(reader: JsonReader): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, value: String) {
    writer.value(value)
  }
}
