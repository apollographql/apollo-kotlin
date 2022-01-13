package com.example

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
object UrlAdapter : Adapter<java.lang.String> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): java.lang.String {
    return reader.nextString()!! as java.lang.String
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: java.lang.String) {
    writer.value(value as String)
  }
}
