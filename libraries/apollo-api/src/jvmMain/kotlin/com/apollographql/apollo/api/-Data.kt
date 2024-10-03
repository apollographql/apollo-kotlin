package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.api.json.buildJsonString

private fun Operation.Data.adapter(): Adapter<Operation.Data> {
  val name = this::class.java.name

  val operationQualifiedName = name.removeSuffix("${'$'}Data")
  val operationName = operationQualifiedName.substringAfterLast(".")
  val packageName = operationQualifiedName.substringBeforeLast(".")

  val adapterName = "$packageName.adapter.${operationName}_ResponseAdapter${'$'}Data"

  val clazz = Class.forName(adapterName)

  val field = clazz.getDeclaredField("INSTANCE")

  @Suppress("UNCHECKED_CAST")
  val adapter = field.get(null) as Adapter<Operation.Data>

  return adapter.obj()
}

actual fun Operation.Data.toJson(jsonWriter: JsonWriter, customScalarAdapters: CustomScalarAdapters) {
  adapter().toJson(jsonWriter, customScalarAdapters, this)
}

/**
 * Serializes the given `Data` to a string.
 *
 * Note: this method uses reflection to lookup the adapter. If you are using R8, add the following rules:
 *
 * ```
 * -keep class ** implements com.apollographql.apollo.api.Operation$Data
 * -keep class **.*_ResponseAdapter$Data {
 *     public static ** INSTANCE;
 * }
 * ```
 *
 * @param customScalarAdapters the adapters to use for custom scalars
 */
@ApolloExperimental
fun Operation.Data.toJsonString(customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty, indent: String? = null): String {
  return buildJsonString(indent) {
    toJson(this, customScalarAdapters)
  }
}

