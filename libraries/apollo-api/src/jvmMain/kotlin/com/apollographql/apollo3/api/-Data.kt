package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.buildJsonString

private fun Operation.Data.adapter(): ApolloAdapter<Operation.Data> {
  val name = this::class.java.name

  val operationQualifiedName = name.removeSuffix("${'$'}Data")
  val operationName = operationQualifiedName.substringAfterLast(".")
  val packageName = operationQualifiedName.substringBeforeLast(".")

  val adapterName = "$packageName.adapter.${operationName}_ResponseAdapter${'$'}Data"

  val clazz = Class.forName(adapterName)

  val field = clazz.getDeclaredField("INSTANCE")

  @Suppress("UNCHECKED_CAST")
  val adapter = field.get(null) as ApolloAdapter<Operation.Data>

  return adapter.obj()
}

actual fun Operation.Data.toJson(jsonWriter: JsonWriter, scalarAdapters: ScalarAdapters) {
  adapter().toJson(jsonWriter, scalarAdapters, this)
}

@ApolloExperimental
fun Operation.Data.toJsonString(scalarAdapters: ScalarAdapters = ScalarAdapters.Empty, indent: String? = null): String {
  return buildJsonString(indent) {
    toJson(this, scalarAdapters)
  }
}

