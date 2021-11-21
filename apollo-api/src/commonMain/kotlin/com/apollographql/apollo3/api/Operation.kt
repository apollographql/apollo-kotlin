package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.http.ApolloHttpRequestComposer
import com.apollographql.apollo3.api.internal.ResponseBodyParser
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.writeObject
import com.apollographql.apollo3.api.json.JsonWriter
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import kotlin.js.JsName
import kotlin.jvm.JvmOverloads

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
  @JsName("operationId")
  fun id(): String

  override fun adapter(): Adapter<D>

  override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters)

  override fun selections(): List<CompiledSelection>

  /**
   * Marker interface for generated models built from data returned by the server in response to this operation.
   */
  interface Data : Executable.Data

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
  fun parseJsonResponse(
      source: BufferedSource,
      customScalarAdapters: CustomScalarAdapters,
  ): ApolloResponse<D> {
    return ResponseBodyParser.parse(source, this, customScalarAdapters)
  }

  /**
   * See [parseJsonResponse]
   */
  fun parseJsonResponse(
      source: BufferedSource,
  ): ApolloResponse<D> {
    return ResponseBodyParser.parse(source, this, CustomScalarAdapters.Empty)
  }


  /**
   * See [parseJsonResponse]
   */
  fun parseJsonResponse(
      byteString: ByteString,
      customScalarAdapters: CustomScalarAdapters,
  ): ApolloResponse<D> {
    return parseJsonResponse(Buffer().write(byteString), customScalarAdapters)
  }

  /**
   * See [parseJsonResponse]
   */
  fun parseJsonResponse(
      byteString: ByteString,
  ): ApolloResponse<D> {
    return parseJsonResponse(Buffer().write(byteString), CustomScalarAdapters.Empty)
  }

  /**
   * See [parseJsonResponse]
   */
  fun parseJsonResponse(
      string: String,
      customScalarAdapters: CustomScalarAdapters,
  ): ApolloResponse<D> {
    return parseJsonResponse(Buffer().writeUtf8(string), customScalarAdapters)
  }

  /**
   * See [parseJsonResponse]
   */
  fun parseJsonResponse(
      string: String,
  ): ApolloResponse<D> {
    return parseJsonResponse(Buffer().writeUtf8(string), CustomScalarAdapters.Empty)
  }

  /**
   * Reads only the "data" part of a GraphQL Json response
   */
  fun parseJsonData(
      source: BufferedSource,
      customScalarAdapters: CustomScalarAdapters,
  ): D {
    return adapter().fromJson(source, customScalarAdapters)
  }

  /**
   * Reads only the "data" part of a GraphQL Json response
   */
  fun parseJsonData(
      source: BufferedSource,
  ): D {
    return adapter().fromJson(source, CustomScalarAdapters.Empty)
  }

  /**
   * see [parseJsonResponse]
   */
  fun parseJsonData(
      byteString: ByteString,
      customScalarAdapters: CustomScalarAdapters,
  ): D {
    return adapter().fromJson(Buffer().write(byteString), customScalarAdapters)
  }

  /**
   * see [parseJsonResponse]
   */
  fun parseJsonData(
      byteString: ByteString,
  ): D {
    return adapter().fromJson(Buffer().write(byteString), CustomScalarAdapters.Empty)
  }

  /**
   * see [parseJsonData]
   */
  fun parseJsonData(
      string: String,
      customScalarAdapters: CustomScalarAdapters,
  ): D {
    return adapter().fromJson(string, customScalarAdapters)
  }

  /**
   * see [parseJsonData]
   */
  fun parseJsonData(
      string: String,
  ): D {
    return adapter().fromJson(string, CustomScalarAdapters.Empty)
  }


  /**
   * writes a successful GraphQL Json response containing "data" to the given sink.
   *
   * Use this for testing/mocking a valid GraphQL response
   */
  fun composeJsonResponse(
      sink: BufferedSink,
      data: D,
      customScalarAdapters: CustomScalarAdapters,
      indent: String,
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
  fun composeJsonResponse(
      sink: BufferedSink,
      data: D,
  ) = composeJsonResponse(sink, data, CustomScalarAdapters.Empty, "  ")

  /**
   * see [composeJsonResponse]
   */
  fun composeJsonResponse(
      data: D,
      customScalarAdapters: CustomScalarAdapters,
      indent: String,
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
   * see [composeJsonResponse]
   */
  fun composeJsonResponse(
      data: D,
  ): String = composeJsonResponse(data, CustomScalarAdapters.Empty, "  ")

  /**
   * writes operation data to the given sink
   *
   * Use this for testing/mocking a valid GraphQL response
   */
  fun composeJsonData(
      sink: BufferedSink,
      data: D,
      customScalarAdapters: CustomScalarAdapters,
      indent: String,
  ) {
    val writer = BufferedSinkJsonWriter(sink)
    writer.indent = indent
    adapter().toJson(writer, customScalarAdapters, data)
  }

  /**
   * See [composeJsonData]
   */
  fun composeJsonData(
      sink: BufferedSink,
      data: D,
  ) = composeJsonData(sink, data, CustomScalarAdapters.Empty, "  ")

  /**
   * See [composeJsonData]
   */
  fun composeJsonData(
      data: D,
      customScalarAdapters: CustomScalarAdapters,
      indent: String,
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
   * See [composeJsonData]
   */
  fun composeJsonData(
      data: D,
  ): String = composeJsonData(data, CustomScalarAdapters.Empty, "  ")


  /**
   * writes the body of a GraphQL json request like below:
   * {
   *  "query": ...
   *  "variables": ...
   *  "extensions": ...
   * }
   */
  fun composeJsonRequest(
      sink: BufferedSink,
      customScalarAdapters: CustomScalarAdapters,
  ) {
    val composer = ApolloHttpRequestComposer("unused")

    val request = composer.compose(
        ApolloRequest.Builder(operation = this)
            .addExecutionContext(customScalarAdapters)
            .build()
    )

    request.body!!.writeTo(sink)
  }

  /**
   * See [composeJsonRequest]
   */
  fun composeJsonRequest(
      sink: BufferedSink,
  ): Unit = composeJsonRequest(sink, CustomScalarAdapters.Empty)

  /**
   * see [composeJsonRequest]
   */
  fun composeJsonRequest(
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
  ): String {
    return Buffer().apply {
      composeJsonRequest(this, customScalarAdapters)
    }.readUtf8()
  }

  /**
   * see [composeJsonRequest]
   */
  fun composeJsonRequest(): String = composeJsonRequest(CustomScalarAdapters.Empty)
}

