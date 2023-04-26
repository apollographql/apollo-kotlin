package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException
import kotlin.jvm.JvmField

/**
 * A [VariablesAdapter] is responsible for adapting GraphQL types used in variables to their Json representation.
 * This class is implemented by the generated code, it shouldn't be used directly.
 */
interface VariablesAdapter<T> {
  @Throws(IOException::class)
  fun serializeVariables(writer: JsonWriter, value: T, context: SerializeVariablesContext)

  class SerializeVariablesContext(
      @JvmField
      val customScalarAdapters: CustomScalarAdapters,

      @JvmField
      val withDefaultBooleanValues: Boolean,
  )
}
