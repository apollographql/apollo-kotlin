package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.internal.ResponseBodyParser
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.writeObject
import com.apollographql.apollo3.api.json.JsonWriter
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString

/**
 * Represents a GraphQL operation (mutation, query or subscription).
 */
interface Operation<D : Operation.Data> : Executable<D> {
  /**
   * The GraphQL operation String to be sent to the server. This might differ from the input `*.graphql` file with:
   * - whitespaces removed
   * - Apollo client directives like `@nonnull` removed
   * - `typename` fields added for polymorphic/fragment cases
   */
  fun document(): String

  /**
   * The GraphQL operation name as in the `*.graphql` file.
   */
  fun name(): String

  /**
   * An unique identifier for the operation. Used for Automatic Persisted Queries. You can customize it with a [OperationIdGenerator]
   */
  fun id(): String

  override fun adapter(): Adapter<D>

  override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters)

  override fun selections(): List<CompiledSelection>

  /**
   * Marker interface for generated models built from data returned by the server in response to this operation.
   */
  interface Data : Executable.Data
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
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    source: BufferedSource,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): ApolloResponse<D> {
  return ResponseBodyParser.parse(source, this, customScalarAdapters)
}

/**
 * See [parseJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    byteString: ByteString,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): ApolloResponse<D> {
  return parseJsonResponse(Buffer().write(byteString), customScalarAdapters)
}

/**
 * See [parseJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    string: String,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): ApolloResponse<D> {
  return parseJsonResponse(Buffer().writeUtf8(string), customScalarAdapters)
}

/**
 * Reads only the "data" part of a GraphQL Json response
 */
fun <D : Operation.Data> Operation<D>.parseJsonData(
    source: BufferedSource,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): D {
  return adapter().fromJson(source, customScalarAdapters)
}

/**
 * see [parseJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.parseJsonData(
    byteString: ByteString,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): D {
  return adapter().fromJson(Buffer().write(byteString), customScalarAdapters)
}

/**
 * see [parseJsonData]
 */
fun <D : Operation.Data> Operation<D>.parseJsonData(
    string: String,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): D {
  return adapter().fromJson(string, customScalarAdapters)
}


/**
 * writes a successful GraphQL Json response containing "data" to the given sink.
 *
 * Use this for testing/mocking a valid GraphQL response
 */
fun <D : Operation.Data> Operation<D>.composeJsonResponse(
    sink: BufferedSink,
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    indent: String = "",
) {
  val writer = BufferedSinkJsonWriter(sink)
  writer.indent = indent
  writer.writeObject {
    name("data")
    adapter().toJson(this, customScalarAdapters, data)
  }
}

/**
 * see [composeJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.composeJsonResponse(
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    indent: String = "",
): String {
  val buffer = Buffer()

  composeJsonResponse(
      sink = buffer,
      data = data,
      customScalarAdapters = customScalarAdapters,
      indent = indent
  )

  return buffer.readUtf8()
}

/**
 * writes operation data to the given sink
 *
 * Use this for testing/mocking a valid GraphQL response
 */
fun <D : Operation.Data> Operation<D>.composeJsonData(
    sink: BufferedSink,
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    indent: String = "",
) {
  val writer = BufferedSinkJsonWriter(sink)
  writer.indent = indent
  adapter().toJson(writer, customScalarAdapters, data)
}

/**
 * See [composeJsonData]
 */
fun <D : Operation.Data> Operation<D>.composeJsonData(
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    indent: String = "",
): String {
  val buffer = Buffer()
  composeJsonData(
      sink = buffer,
      data = data,
      customScalarAdapters = customScalarAdapters,
      indent = indent
  )
  return buffer.readUtf8()
}


/**
 * writes the body of a GraphQL json request like below:
 * {
 *  "query": ...
 *  "variables": ...
 *  "extensions": ...
 * }
 */
fun <D : Operation.Data> Operation<D>.composeJsonRequest(
    sink: BufferedSink,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
) {
  val composer = DefaultHttpRequestComposer("unused")

  val request = composer.compose(
      ApolloRequest(operation = this)
          .withExecutionContext(customScalarAdapters)
  )

  request.body!!.writeTo(sink)
}

/**
 * see [composeJsonRequest]
 */
fun <D : Operation.Data> Operation<D>.composeJsonRequest(
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): String {
  return Buffer().apply {
    composeJsonRequest(this, customScalarAdapters)
  }.readUtf8()
}

