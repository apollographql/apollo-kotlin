@file:JvmName("Executables")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.MapJsonWriter
import okio.Buffer
import kotlin.jvm.JvmName

fun <D : Executable.Data> Executable<D>.variables(customScalarAdapters: CustomScalarAdapters): Executable.Variables {
  return variables(customScalarAdapters, false)
}

fun <D : Executable.Data> Executable<D>.variablesJson(customScalarAdapters: CustomScalarAdapters): String {
  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).apply {
    beginObject()
    serializeVariables(this, customScalarAdapters.serializeVariablesWithDefaultBooleanValues())
    endObject()
  }
  return buffer.readUtf8()
}

@Suppress("UNCHECKED_CAST")
@ApolloInternal
fun <D : Executable.Data> Executable<D>.variables(
    customScalarAdapters: CustomScalarAdapters,
    withDefaultBooleanValues: Boolean,
): Executable.Variables {
  val valueMap = MapJsonWriter().apply {
    beginObject()
    serializeVariables(this, customScalarAdapters.let { if (withDefaultBooleanValues) it.serializeVariablesWithDefaultBooleanValues() else it })
    endObject()
  }.root() as Map<String, Any?>
  return Executable.Variables(valueMap)
}

private fun CustomScalarAdapters.serializeVariablesWithDefaultBooleanValues() = newBuilder()
    .adapterContext(
        adapterContext.newBuilder()
            .serializeVariablesWithDefaultBooleanValues(true)
            .build()
    )
    .build()
