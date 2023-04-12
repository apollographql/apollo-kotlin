package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException

/**
 * An [ApolloAdapter] is responsible for adapting Kotlin-generated GraphQL types to/from their Json representation.
 *
 * It is used to
 * - deserialize network responses
 * - normalize models into records that can be stored in cache
 * - deserialize records
 *
 * This class is implemented by the generated code, it shouldn't be used directly.
 */
interface ApolloAdapter<T> {
  @Throws(IOException::class)
  fun fromJson(reader: JsonReader, context: DataDeserializeContext): T

  @Throws(IOException::class)
  fun toJson(writer: JsonWriter, value: T, context: DataSerializeContext)

  class DataSerializeContext(
      val scalarAdapters: ScalarAdapters,
  )

  class DataDeserializeContext(
      val scalarAdapters: ScalarAdapters,
      val booleanFalseVariables: Set<String>?,
      val mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>?,
  )
}

fun <T> ApolloAdapter<T>.toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: T) {
  toJson(writer, value, ApolloAdapter.DataSerializeContext(scalarAdapters))
}
