package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonWriter

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

fun Operation.Data.toJson(jsonWriter: JsonWriter, customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty) {
  adapter().toJson(jsonWriter, customScalarAdapters, this)
}

