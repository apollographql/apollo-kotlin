package com.apollographql.apollo3.api.java.adapter

import com.apollographql.apollo3.api.CompositeAdapter
import com.apollographql.apollo3.api.CompositeAdapter.DeserializeCompositeContext
import com.apollographql.apollo3.api.CompositeAdapter.SerializeCompositeContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.google.common.base.Optional

/**
 * A composite adapter for Guava's [Optional]. `null` is deserialized as [Optional.absent].
 */
class GuavaOptionalCompositeAdapter<T : Any>(private val wrappedAdapter: CompositeAdapter<T>) : CompositeAdapter<Optional<T>> {
  override fun deserializeComposite(reader: JsonReader, context: DeserializeCompositeContext): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.absent()
    } else {
      Optional.of(wrappedAdapter.deserializeComposite(reader, context))
    }
  }

  override fun serializeComposite(writer: JsonWriter, value: Optional<T>, context: SerializeCompositeContext) {
    if (!value.isPresent) {
      writer.nullValue()
    } else {
      wrappedAdapter.serializeComposite(writer, value.get(), context)
    }
  }
}
