@file:JvmName("Executables")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.MapJsonWriter
import okio.Buffer
import kotlin.jvm.JvmName

@Suppress("UNCHECKED_CAST")
fun <D : Executable.Data> Executable<D>.variables(customScalarAdapters: CustomScalarAdapters): Executable.Variables {
  val valueMap = MapJsonWriter().apply {
    beginObject()
    serializeVariables(this, customScalarAdapters)
    endObject()
  }.root() as Map<String, Any?>
  return Executable.Variables(valueMap)
}

fun <D : Executable.Data> Executable<D>.variablesJson(customScalarAdapters: CustomScalarAdapters): String {
  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).apply {
    beginObject()
    serializeVariables(this, customScalarAdapters)
    endObject()
  }
  return buffer.readUtf8()
}
