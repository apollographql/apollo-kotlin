@file:JvmName("GuavaOptionalAdapters")

package com.apollographql.apollo3.api.java.adapter


import com.apollographql.apollo3.api.AnyApolloAdapter
import com.apollographql.apollo3.api.ApolloAdapter
import com.apollographql.apollo3.api.BooleanApolloAdapter
import com.apollographql.apollo3.api.DoubleApolloAdapter
import com.apollographql.apollo3.api.IntApolloAdapter
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.StringApolloAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.google.common.base.Optional

/**
 * An adapter for Guava's [Optional]. `null` is deserialized as [Optional.absent].
 */
class GuavaOptionalAdapter<T : Any>(private val wrappedAdapter: ApolloAdapter<T>) : ApolloAdapter<Optional<T>> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.absent()
    } else {
      Optional.of(wrappedAdapter.fromJson(reader, scalarAdapters))
    }
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Optional<T>) {
    if (!value.isPresent) {
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, scalarAdapters, value.get())
    }
  }
}

/*
 * Global instances of optional adapters for built-in scalar types
 */
@JvmField
val GuavaOptionalStringAdapter = GuavaOptionalAdapter(StringApolloAdapter)

@JvmField
val GuavaOptionalDoubleAdapter = GuavaOptionalAdapter(DoubleApolloAdapter)

@JvmField
val GuavaOptionalIntAdapter = GuavaOptionalAdapter(IntApolloAdapter)

@JvmField
val GuavaOptionalBooleanAdapter = GuavaOptionalAdapter(BooleanApolloAdapter)

@JvmField
val GuavaOptionalAnyAdapter = GuavaOptionalAdapter(AnyApolloAdapter)
