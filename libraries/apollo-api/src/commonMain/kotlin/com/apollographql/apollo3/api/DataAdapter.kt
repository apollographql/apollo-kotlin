package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.DataAdapter.DeserializeDataContext
import com.apollographql.apollo3.api.DataAdapter.SerializeDataContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException
import kotlin.jvm.JvmField

/**
 * A [DataAdapter] is responsible for adapting Kotlin-generated GraphQL types to/from their Json representation.
 *
 * It is used to
 * - deserialize network responses
 * - normalize models into records that can be stored in cache
 * - deserialize records
 *
 * This class is implemented by the generated code, it shouldn't be used directly.
 */
interface DataAdapter<T> {
  @Throws(IOException::class)
  fun deserializeData(reader: JsonReader, context: DeserializeDataContext): T

  @Throws(IOException::class)
  fun serializeData(writer: JsonWriter, value: T, context: SerializeDataContext)

  class SerializeDataContext(
      @JvmField
      val customScalarAdapters: CustomScalarAdapters,
  )

  class DeserializeDataContext(
      @JvmField
      val customScalarAdapters: CustomScalarAdapters,

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

fun <T> DataAdapter<T>.toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
  serializeData(writer, value, SerializeDataContext(customScalarAdapters))
}

fun <T> DataAdapter<T>.fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
  return deserializeData(reader, DeserializeDataContext(customScalarAdapters = customScalarAdapters, falseBooleanVariables = emptySet(), mergedDeferredFragmentIds = null))
}