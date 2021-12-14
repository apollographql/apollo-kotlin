package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.*
import com.apollographql.apollo3.api.NullableAnyAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

/**
 * An [Adapter] that wraps an Apollo Android v2 style [CustomTypeAdapter], to ease migration from v2 to v3.
 */
@Suppress("DEPRECATION")
@ApolloInternal
class Version2CustomTypeAdapterToAdapter<T>(
    private val v2CustomTypeAdapter: CustomTypeAdapter<T>,
) : Adapter<T> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
    val value: Any? = NullableAnyAdapter.fromJson(reader, customScalarAdapters)
    return v2CustomTypeAdapter.decode(CustomTypeValue.fromRawValue(value))
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
    val encoded: Any? = v2CustomTypeAdapter.encode(value).value
    NullableAnyAdapter.toJson(writer, customScalarAdapters, encoded)
  }
}
