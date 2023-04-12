@file:JvmName("Operations")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.VariablesAdapter.SerializeVariablesContext
import com.apollographql.apollo3.api.internal.ResponseParser
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.exception.JsonDataException
import com.apollographql.apollo3.exception.JsonEncodingException
import okio.Buffer
import okio.IOException
import okio.use
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * Reads a GraphQL Json response like below to a [ApolloResponse]
 * ```
 * {
 *  "data": ...
 *  "errors": ...
 *  "extensions": ...
 * }
 * ```
 */
@JvmOverloads
fun <D : Operation.Data> Operation<D>.composeJsonRequest(
    jsonWriter: JsonWriter,
    scalarAdapters: ScalarAdapters = ScalarAdapters.Empty,
) {
  jsonWriter.writeObject {
    name("operationName")
    value(name())

    name("variables")
    writeObject {
      serializeVariables(this, SerializeVariablesContext(scalarAdapters, withDefaultBooleanValues = false))
    }

    name("query")
    value(document())
  }
}

/**
 * Reads a GraphQL Json response like below to a [ApolloResponse]
 * ```
 * {
 *  "data": ...
 *  "errors": ...
 *  "extensions": ...
 * }
 * ```
 *
 * This method takes ownership of [jsonReader] and will always close it
 *
 * @throws IOException if reading [jsonReader] fails
 * @throws JsonEncodingException if the data is not valid json
 * @throws JsonDataException if the data is not of the expected type
 */
@JvmOverloads
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    jsonReader: JsonReader,
    scalarAdapters: ScalarAdapters = ScalarAdapters.Empty,
    mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>? = null,
): ApolloResponse<D> {
  val variables = booleanVariables(scalarAdapters)
  return ResponseParser.parse(
      jsonReader,
      this,
      ApolloAdapter.DataDeserializeContext(
          scalarAdapters = scalarAdapters,
          falseBooleanVariables = variables,
          mergedDeferredFragmentIds = mergedDeferredFragmentIds,
      )
  )
}

@JvmOverloads
@ApolloExperimental
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    json: String,
    scalarAdapters: ScalarAdapters = ScalarAdapters.Empty,
): ApolloResponse<D> {
  return parseJsonResponse(Buffer().writeUtf8(json).jsonReader(), scalarAdapters)
}

/**
 * writes a successful GraphQL Json response containing "data" to the given sink.
 *
 * Use this for testing/mocking a valid GraphQL response
 */
@JvmOverloads
fun <D : Operation.Data> Operation<D>.composeJsonResponse(
    jsonWriter: JsonWriter,
    data: D,
    scalarAdapters: ScalarAdapters = ScalarAdapters.Empty,
) {
  jsonWriter.use {
    it.writeObject {
      name("data")
      adapter().toJson(this, scalarAdapters, data)
    }
  }
}

