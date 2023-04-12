@file:JvmName("GuavaOptionalAdapters")

package com.apollographql.apollo3.api.java.adapter


import com.apollographql.apollo3.api.AnyApolloAdapter
import com.apollographql.apollo3.api.ApolloAdapter
import com.apollographql.apollo3.api.ApolloAdapter.DataDeserializeContext
import com.apollographql.apollo3.api.ApolloAdapter.DataSerializeContext
import com.apollographql.apollo3.api.BooleanApolloAdapter
import com.apollographql.apollo3.api.DoubleApolloAdapter
import com.apollographql.apollo3.api.IntApolloAdapter
import com.apollographql.apollo3.api.StringApolloAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.google.common.base.Optional

/**
 * An adapter for Guava's [Optional]. `null` is deserialized as [Optional.absent].
 */
class GuavaOptionalAdapter<T : Any>(private val wrappedAdapter: ApolloAdapter<T>) : ApolloAdapter<Optional<T>> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.absent()
    } else {
      Optional.of(wrappedAdapter.fromJson(reader, context))
    }
  }

  override fun toJson(writer: JsonWriter, value: Optional<T>, context: DataSerializeContext) {
    if (!value.isPresent) {
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, value.get(), context)
    }
  }
}

/*
 * Global instances of optional adapters for built-in scalar types
 */
@JvmField
val GuavaOptionalStringApolloAdapter = GuavaOptionalAdapter(StringApolloAdapter)

@JvmField
val GuavaOptionalDoubleApolloAdapter = GuavaOptionalAdapter(DoubleApolloAdapter)

@JvmField
val GuavaOptionalIntApolloAdapter = GuavaOptionalAdapter(IntApolloAdapter)

@JvmField
val GuavaOptionalBooleanApolloAdapter = GuavaOptionalAdapter(BooleanApolloAdapter)

@JvmField
val GuavaOptionalAnyApolloAdapter = GuavaOptionalAdapter(AnyApolloAdapter)
