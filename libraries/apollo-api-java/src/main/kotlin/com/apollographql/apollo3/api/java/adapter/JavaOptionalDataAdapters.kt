@file:JvmName("JavaOptionalDataAdapters")

package com.apollographql.apollo3.api.java.adapter

import com.apollographql.apollo3.api.DataAdapter
import com.apollographql.apollo3.api.DataAdapter.DeserializeDataContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import java.util.Optional

/**
 * A data adapter for Java's [Optional]. `null` is deserialized as [Optional.empty].
 */
class JavaOptionalDataAdapter<T : Any>(private val wrappedAdapter: DataAdapter<T>) : DataAdapter<Optional<T>> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.empty()
    } else {
      Optional.of(wrappedAdapter.deserializeData(reader, context))
    }
  }

  override fun serializeData(writer: JsonWriter, value: Optional<T>, context: DataAdapter.SerializeDataContext) {
    if (!value.isPresent) {
      writer.nullValue()
    } else {
      wrappedAdapter.serializeData(writer, value.get(), context)
    }
  }
}
