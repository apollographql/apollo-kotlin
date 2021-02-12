package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter

class NullableResponseAdapter<T:Any>(private val wrappedAdapter: ResponseAdapter<T>): ResponseAdapter<T?> {
  override fun fromResponse(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T? {
    return if (reader.peek() == JsonReader.Token.NULL) {
      null
    } else {
      wrappedAdapter.fromResponse(reader, customScalarAdapters)
    }
  }

  override fun toResponse(writer: JsonWriter, value: T?, customScalarAdapters: CustomScalarAdapters) {
    if (value == null) {
      writer.nullValue()
    } else {
      wrappedAdapter.toResponse(writer, value, customScalarAdapters)
    }
  }
}