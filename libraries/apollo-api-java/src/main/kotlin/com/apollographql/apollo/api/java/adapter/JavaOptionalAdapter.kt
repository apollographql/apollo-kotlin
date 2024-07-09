@file:JvmName("JavaOptionalAdapters")

package com.apollographql.apollo.api.java.adapter

import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.AnyAdapter
import com.apollographql.apollo.api.BooleanAdapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.DoubleAdapter
import com.apollographql.apollo.api.IntAdapter
import com.apollographql.apollo.api.StringAdapter
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import java.util.Optional

/**
 * An adapter for Java's [Optional]. `null` is deserialized as [Optional.empty].
 */
class JavaOptionalAdapter<T : Any>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional<T>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.empty()
    } else {
      Optional.of(wrappedAdapter.fromJson(reader, customScalarAdapters))
    }
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Optional<T>) {
    if (!value.isPresent) {
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, customScalarAdapters, value.get())
    }
  }
}

/*
 * Global instances of optional adapters for built-in scalar types
 */
@JvmField
val JavaOptionalStringAdapter = JavaOptionalAdapter(StringAdapter)

@JvmField
val JavaOptionalDoubleAdapter = JavaOptionalAdapter(DoubleAdapter)

@JvmField
val JavaOptionalIntAdapter = JavaOptionalAdapter(IntAdapter)

@JvmField
val JavaOptionalBooleanAdapter = JavaOptionalAdapter(BooleanAdapter)

@JvmField
val JavaOptionalAnyAdapter = JavaOptionalAdapter(AnyAdapter)
