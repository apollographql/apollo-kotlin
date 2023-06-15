package com.apollographql.apollo3.api.java.adapter

import com.apollographql.apollo3.api.CompositeAdapter
import com.apollographql.apollo3.api.CompositeAdapterContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import java.util.Optional

/**
 * A composite adapter for Java's [Optional]. `null` is deserialized as [Optional.empty].
 */
class JavaOptionalCompositeAdapter<T : Any>(private val wrappedAdapter: CompositeAdapter<T>) : CompositeAdapter<Optional<T>> {
  override fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.empty()
    } else {
      Optional.of(wrappedAdapter.fromJson(reader, adapterContext))
    }
  }

  override fun toJson(writer: JsonWriter, value: Optional<T>, adapterContext: CompositeAdapterContext) {
    if (!value.isPresent) {
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, value.get(), adapterContext)
    }
  }
}
