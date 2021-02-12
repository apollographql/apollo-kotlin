package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter

class ListResponseAdapter<T>(private val wrappedAdapter: ResponseAdapter<T>): ResponseAdapter<List<T>> {
  override fun fromResponse(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): List<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while(reader.hasNext()) {
      list.add(wrappedAdapter.fromResponse(reader, customScalarAdapters))
    }
    reader.endArray()
    return list
  }

  override fun toResponse(writer: JsonWriter, value: List<T>, customScalarAdapters: CustomScalarAdapters) {
    value.forEach {
      wrappedAdapter.toResponse(writer, it, customScalarAdapters)
    }
  }
}
