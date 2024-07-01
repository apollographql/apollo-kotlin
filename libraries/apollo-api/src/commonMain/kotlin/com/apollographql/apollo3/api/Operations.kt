@file:JvmName("Operations")

package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.internal.ResponseParser
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.api.json.writeObject
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.JsonDataException
import com.apollographql.apollo.exception.JsonEncodingException
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
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
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
) {
  jsonWriter.writeObject {
    name("operationName")
    value(name())

    name("variables")
    writeObject {
      serializeVariables(this, customScalarAdapters, false)
    }

    name("query")
    value(document())
  }
}

/**
 * Reads a GraphQL Json response to a [ApolloResponse]. GraphQL Json responses look like so:
 *
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
@Deprecated("Use parseResponse or jsonReader.toApolloResponse() instead", ReplaceWith("parseResponse()"))
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    jsonReader: JsonReader,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>? = null,
): ApolloResponse<D> {
  return jsonReader.use {
    ResponseParser.parse(
        it,
        this,
        null,
        customScalarAdapters,
        deferredFragmentIdentifiers,
    )
  }
}

/**
 * Reads a GraphQL Json response like below to a [ApolloResponse]. GraphQL Json responses look like so:
 *
 * ```
 * {
 *  "data": ...
 *  "errors": ...
 *  "extensions": ...
 * }
 * ```
 *
 * By default, this method does not close the [jsonReader]
 *
 * @see [toApolloResponse]
 */
@JvmOverloads
fun <D : Operation.Data> Operation<D>.parseResponse(
    jsonReader: JsonReader,
    requestUuid: Uuid? = null,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>? = null,
): ApolloResponse<D> {
  return try {
    ResponseParser.parse(
        jsonReader,
        this,
        requestUuid,
        customScalarAdapters,
        deferredFragmentIdentifiers,
    )
  } catch (throwable: Throwable) {
    ApolloResponse.Builder(requestUuid = requestUuid ?: uuid4(), operation = this)
        .exception(exception = throwable.wrapIfNeeded())
        .isLast(true)
        .build()
  }
}

private fun Throwable.wrapIfNeeded(): ApolloException {
  return if (this is ApolloException) {
    this
  } else {
    ApolloNetworkException(
        message = "Error while reading JSON response",
        platformCause = this
    )
  }
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
  jsonWriter.use {
    it.writeObject {
      name("data")
      adapter().toJson(this, customScalarAdapters, data)
    }
  }
}

@ApolloExperimental
fun <D : Operation.Data> Operation<D>.composeJsonResponse(
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): String {
  return buildJsonString {
    writeObject {
      name("data")
      adapter().toJson(this, customScalarAdapters, data)
    }
  }
}

/**
 * Reads a single [ApolloResponse] from [this]. Returns an error response if [this] contains
 * more than one JSON response or trailing tokens.
 * [toApolloResponse] takes ownership and closes [this].
 *
 * @return the parsed [ApolloResponse]
 * @see parseResponse
 */
@ApolloExperimental
fun <D : Operation.Data> JsonReader.toApolloResponse(
    operation: Operation<D>,
    requestUuid: Uuid? = null,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>? = null,
): ApolloResponse<D> {
  return use {
    try {
      ResponseParser.parse(
          this,
          operation,
          requestUuid,
          customScalarAdapters,
          deferredFragmentIdentifiers,
      ).also {
        if (peek() != JsonReader.Token.END_DOCUMENT) {
          throw JsonDataException("Expected END_DOCUMENT but was ${peek()}")
        }
      }
    } catch (throwable: Throwable) {
      ApolloResponse.Builder(requestUuid = requestUuid ?: uuid4(), operation = operation)
          .exception(exception = throwable.wrapIfNeeded())
          .isLast(true)
          .build()
    }
  }
}

/**
 * Reads a [ApolloResponse] from [this].
 * The caller is responsible for closing [this].
 *
 * @return the parsed [ApolloResponse]
 * @see [toApolloResponse]
 */
@ApolloExperimental
fun <D : Operation.Data> JsonReader.parseResponse(
    operation: Operation<D>,
    requestUuid: Uuid? = null,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>? = null,
): ApolloResponse<D> {
  return try {
    ResponseParser.parse(
        this,
        operation,
        requestUuid,
        customScalarAdapters,
        deferredFragmentIdentifiers,
    )
  } catch (throwable: Throwable) {
    ApolloResponse.Builder(requestUuid = requestUuid ?: uuid4(), operation = operation)
        .exception(exception = throwable.wrapIfNeeded())
        .isLast(true)
        .build()
  }
}

