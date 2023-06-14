package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.ApolloAdapter.DataDeserializeContext
import com.apollographql.apollo3.api.ApolloAdapter.DataSerializeContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException
import kotlin.jvm.JvmField

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
  fun deserializeData(reader: JsonReader, context: DataDeserializeContext): T

  @Throws(IOException::class)
  fun serializeData(writer: JsonWriter, value: T, context: DataSerializeContext)

  class DataSerializeContext(
      @JvmField
      val scalarAdapters: ScalarAdapters,
  )

  class DataDeserializeContext(
      @JvmField
      val scalarAdapters: ScalarAdapters,

      @JvmField
      val falseBooleanVariables: Set<String>,

      @JvmField
      val mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>?,
  ) {
    fun hasDeferredFragment(path: List<Any>, label: String?): Boolean {
      if (mergedDeferredFragmentIds == null) {
        // By default, parse all deferred fragments - this is the case when parsing from the normalized cache.
        return true
      }
      return mergedDeferredFragmentIds.contains(DeferredFragmentIdentifier(path, label))
    }
  }
}

fun <T> ApolloAdapter<T>.toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: T) {
  serializeData(writer, value, DataSerializeContext(scalarAdapters))
}

fun <T> ApolloAdapter<T>.fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): T {
  return deserializeData(reader, DataDeserializeContext(scalarAdapters = scalarAdapters, falseBooleanVariables = emptySet(), mergedDeferredFragmentIds = null))
}
