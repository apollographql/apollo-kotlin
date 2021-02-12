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

object stringResponseAdapter: ResponseAdapter<String> {
  override fun fromResponse(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
    return reader.nextString()!!
  }

  override fun toResponse(writer: JsonWriter, value: String, customScalarAdapters: CustomScalarAdapters) {
    writer.value(value)
  }
}

object intResponseAdapter: ResponseAdapter<Int> {
  override fun fromResponse(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Int {
    return reader.nextInt()
  }

  override fun toResponse(writer: JsonWriter, value: Int, customScalarAdapters: CustomScalarAdapters) {
    writer.value(value)
  }
}

object doubleResponseAdapter: ResponseAdapter<Double> {
  override fun fromResponse(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Double {
    return reader.nextDouble()
  }

  override fun toResponse(writer: JsonWriter, value: Double, customScalarAdapters: CustomScalarAdapters) {
    writer.value(value)
  }
}

object booleanResponseAdapter: ResponseAdapter<Boolean> {
  override fun fromResponse(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Boolean {
    return reader.nextBoolean()
  }

  override fun toResponse(writer: JsonWriter, value: Boolean, customScalarAdapters: CustomScalarAdapters) {
    writer.value(value)
  }
}
