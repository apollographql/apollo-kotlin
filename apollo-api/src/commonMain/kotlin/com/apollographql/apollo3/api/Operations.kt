@file:JvmName("Operations")
package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.internal.ResponseParser
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.writeObject
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
@OptIn(ApolloInternal::class)
@JvmOverloads
fun <D : Operation.Data> Operation<D>.composeJsonRequest(
    jsonWriter: JsonWriter,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
) {
  jsonWriter.writeObject {
    name("operationName")
    value(name())

    name("variables")
    writeObject {
      serializeVariables(this, customScalarAdapters)
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
 */
@JvmOverloads
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    jsonReader: JsonReader,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): ApolloResponse<D> {
  return ResponseParser.parse(jsonReader, this, customScalarAdapters)
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
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
) {
  @OptIn(ApolloInternal::class)
  jsonWriter.use {
    it.writeObject {
      name("data")
      adapter().toJson(this, customScalarAdapters, data)
    }
  }
}
