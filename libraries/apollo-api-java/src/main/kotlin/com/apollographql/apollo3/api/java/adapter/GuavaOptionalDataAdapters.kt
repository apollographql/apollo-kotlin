@file:JvmName("GuavaOptionalDataAdapters")

package com.apollographql.apollo3.api.java.adapter


import com.apollographql.apollo3.api.DataAdapter
import com.apollographql.apollo3.api.DataAdapter.DeserializeDataContext
import com.apollographql.apollo3.api.DataAdapter.SerializeDataContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.google.common.base.Optional

/**
 * A data adapter for Guava's [Optional]. `null` is deserialized as [Optional.absent].
 */
class GuavaOptionalDataAdapter<T : Any>(private val wrappedAdapter: DataAdapter<T>) : DataAdapter<Optional<T>> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.absent()
    } else {
      Optional.of(wrappedAdapter.deserializeData(reader, context))
    }
  }

  override fun serializeData(writer: JsonWriter, value: Optional<T>, context: SerializeDataContext) {
    if (!value.isPresent) {
      writer.nullValue()
    } else {
      wrappedAdapter.serializeData(writer, value.get(), context)
    }
  }
}
