@file:JvmName("Executables")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.VariablesAdapter.SerializeVariablesContext
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.MapJsonWriter
import okio.Buffer
import kotlin.jvm.JvmName


/**
 * Returns a map of the variables as they would be sent over the wire. Use this to construct your own HTTP requests
 */
fun <D : Executable.Data> Executable<D>.variables(scalarAdapters: ScalarAdapters): Executable.Variables {
  return variables(scalarAdapters, false)
}

/**
 * Returns the variables as they would be sent over the wire. Use this to construct your own HTTP requests
 */
fun <D : Executable.Data> Executable<D>.variablesJson(scalarAdapters: ScalarAdapters): String {
  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).apply {
    beginObject()
    serializeVariables(this, SerializeVariablesContext(scalarAdapters = scalarAdapters, withDefaultBooleanValues = false))
    endObject()
  }
  return buffer.readUtf8()
}

/**
 * Returns the [Set] of boolean variables that are **false** either explicitly or because there is a default value
 *
 * This inversion is needed for `@defer(if: Boolean! = true)` that default to true so absence of a value in the set
 * denotes true
 *
 * Order of precedence for a query like this: query($foo: Boolean = false)
 * - variables: {"foo": true} => bit not set
 * - variables: {"foo": false} => bit set
 * - variables: {} => bit set
 * Order of precedence for a query like this: query($foo: Boolean)
 * - variables: {"foo": true} => bit not set
 * - variables: {"foo": false} => bit set
 * - variables: {} => bit not set
 */
@ApolloInternal
fun <D : Executable.Data> Executable<D>.booleanVariables(scalarAdapters: ScalarAdapters): Set<String> {
  return variables(scalarAdapters, true).valueMap.filter { it.value == false }.keys
}

@Suppress("UNCHECKED_CAST")
@ApolloInternal
fun <D : Executable.Data> Executable<D>.variables(scalarAdapters: ScalarAdapters, withDefaultBooleanValues: Boolean): Executable.Variables {
  val valueMap = MapJsonWriter().apply {
    beginObject()
    serializeVariables(this, SerializeVariablesContext(scalarAdapters = scalarAdapters, withDefaultBooleanValues = withDefaultBooleanValues))
    endObject()
  }.root() as Map<String, Any?>
  return Executable.Variables(valueMap)
}
