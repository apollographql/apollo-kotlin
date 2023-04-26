package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.CompositeAdapter.DeserializeCompositeContext
import com.apollographql.apollo3.api.CompositeAdapter.SerializeCompositeContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException
import kotlin.jvm.JvmField

/**
 * A [CompositeAdapter] is responsible for adapting Kotlin-generated GraphQL composite types to/from their Json representation.
 *
 * It is used to
 * - deserialize network responses
 * - normalize models into records that can be stored in cache
 * - deserialize records
 *
 * This class is implemented by the generated code, it shouldn't be used directly.
 */
interface CompositeAdapter<T> {
  @Throws(IOException::class)
  fun deserializeComposite(reader: JsonReader, context: DeserializeCompositeContext): T

  @Throws(IOException::class)
  fun serializeComposite(writer: JsonWriter, value: T, context: SerializeCompositeContext)

  class SerializeCompositeContext(
      @JvmField
      val customScalarAdapters: CustomScalarAdapters,
  )

  class DeserializeCompositeContext(
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

fun <T> CompositeAdapter<T>.toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
  serializeComposite(writer, value, SerializeCompositeContext(customScalarAdapters))
}

fun <T> CompositeAdapter<T>.fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
  return deserializeComposite(reader, DeserializeCompositeContext(customScalarAdapters = customScalarAdapters, falseBooleanVariables = emptySet(), mergedDeferredFragmentIds = null))
}
