@file:JvmName("Executables")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import okio.Buffer
import kotlin.jvm.JvmName

/**
 * TODO deprecate it?
 */
fun <D : Executable.Data> Executable<D>.variables(@Suppress("UNUSED_PARAMETER") customScalarAdapters: CustomScalarAdapters): Executable.Variables {
  return variables(withDefaultBooleanValues = true)
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
