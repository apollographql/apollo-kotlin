package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloAdaptableWith
import com.apollographql.apollo.api.json.JsonWriter
import kotlin.reflect.ExperimentalAssociatedObjects
import kotlin.reflect.findAssociatedObject

/**
 *
 */
@OptIn(ExperimentalAssociatedObjects::class)
actual fun Operation.Data.toJson(
    jsonWriter: JsonWriter,
    customScalarAdapters: CustomScalarAdapters,
) {
  @Suppress("UNCHECKED_CAST")
  val adapter = this::class.findAssociatedObject<ApolloAdaptableWith>() as Adapter<Any>

  adapter.obj(false).toJson(jsonWriter, customScalarAdapters, this)
}
