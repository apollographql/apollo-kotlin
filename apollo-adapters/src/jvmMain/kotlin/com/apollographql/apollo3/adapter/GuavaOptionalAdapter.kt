@file:JvmName("GuavaOptionalAdapters")

package com.apollographql.apollo3.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.BooleanAdapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.DoubleAdapter
import com.apollographql.apollo3.api.IntAdapter
import com.apollographql.apollo3.api.StringAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.google.common.base.Optional

/**
 * An adapter for Guava's [Optional]. `Optional.absent()` is serialized as `null`.
 */
class GuavaOptionalAdapter<T : Any>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional<T>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Optional<T> {
    return Optional.fromNullable(wrappedAdapter.fromJson(reader, customScalarAdapters))
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
val GuavaOptionalStringAdapter = GuavaOptionalAdapter(StringAdapter)

@JvmField
val GuavaOptionalDoubleAdapter = GuavaOptionalAdapter(DoubleAdapter)

@JvmField
val GuavaOptionalIntAdapter = GuavaOptionalAdapter(IntAdapter)

@JvmField
val GuavaOptionalBooleanAdapter = GuavaOptionalAdapter(BooleanAdapter)

@JvmField
val GuavaOptionalAnyAdapter = GuavaOptionalAdapter(AnyAdapter)
