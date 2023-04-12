package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException
import kotlin.jvm.JvmField

interface VariablesAdapter<T> {
  @Throws(IOException::class)
  fun serializeVariables(writer: JsonWriter, value: T, context: SerializeVariablesContext)

  class SerializeVariablesContext(
      @JvmField
      val scalarAdapters: ScalarAdapters,

      @JvmField
      val withDefaultBooleanValues: Boolean,
  )
}
