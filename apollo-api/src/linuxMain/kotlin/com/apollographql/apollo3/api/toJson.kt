package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloAdaptableWith
import com.apollographql.apollo3.api.json.JsonWriter
import kotlin.reflect.ExperimentalAssociatedObjects
import kotlin.reflect.findAssociatedObject

@OptIn(ExperimentalAssociatedObjects::class)
actual fun Operation.Data.toJson(
    jsonWriter: JsonWriter,
    customScalarAdapters: CustomScalarAdapters,
) {
  @Suppress("UNCHECKED_CAST")
  val adapter = this::class.findAssociatedObject<ApolloAdaptableWith>() as Adapter<Any>

  adapter.obj(false).toJson(jsonWriter, customScalarAdapters, this)
}