package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpRequestComposerParams
import com.apollographql.apollo3.api.http.HttpMethod
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

  override fun fieldSets(): List<FieldSet>

  /**
   * Marker interface for generated models built from data returned by the server in response to this operation.
   */
  interface Data : Executable.Data
}

fun <D : Operation.Data> Operation<D>.parseResponseBody(
    source: BufferedSource,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): ApolloResponse<D> {
  return ResponseBodyParser.parse(source, this, customScalarAdapters)
}

fun <D : Operation.Data> Operation<D>.parseResponseBody(
    byteString: ByteString,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): ApolloResponse<D> {
  return parseResponseBody(Buffer().write(byteString), customScalarAdapters)
}

fun <D : Operation.Data> Operation<D>.parseResponseBody(
    string: String,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): ApolloResponse<D> {
  return parseResponseBody(Buffer().writeUtf8(string), customScalarAdapters)
}


fun <D : Operation.Data> Operation<D>.composeRequestBody(
    sink: BufferedSink,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
) {
  val composer = DefaultHttpRequestComposer("unused")

  val request = composer.compose(
      ApolloRequest(operation = this)
          .withExecutionContext(customScalarAdapters)
          .withExecutionContext(
              HttpRequestComposerParams(
                  method = HttpMethod.Post,
                  autoPersistQueries = false,
                  sendDocument = true,
                  extraHeaders = emptyMap()
              )
          )
  )

  request.body!!.writeTo(sink)
}

fun <D : Operation.Data> Operation<D>.composeRequestBody(
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): String {
  return Buffer().apply {
    composeRequestBody(this, customScalarAdapters)
  }.readUtf8()
}

fun <D : Operation.Data> Operation<D>.composeResponseBody(
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

fun <D : Operation.Data> Operation<D>.composeResponseBody(
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    indent: String = "",
): String {
  val buffer = Buffer()

  composeResponseBody(
      sink = buffer,
      data = data,
      customScalarAdapters = customScalarAdapters,
      indent = indent
  )

  return buffer.readUtf8()
}

fun <D : Operation.Data> Operation<D>.composeData(
    sink: BufferedSink,
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
) {
  adapter().toJson(BufferedSinkJsonWriter(sink), customScalarAdapters, data)
}

fun <D : Operation.Data> Operation<D>.composeData(
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): String {
  val buffer = Buffer()
  composeData(
      sink = buffer,
      data = data,
      customScalarAdapters = customScalarAdapters
  )
  return buffer.readUtf8()
}

fun <D : Operation.Data> Operation<D>.parseData(
    source: BufferedSource,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): D {
  return adapter().fromJson(source, customScalarAdapters)
}

fun <D : Operation.Data> Operation<D>.parseData(
    byteString: ByteString,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): D {
  return adapter().fromJson(Buffer().write(byteString), customScalarAdapters)
}

fun <D : Operation.Data> Operation<D>.parseData(
    string: String,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): D {
  return adapter().fromJson(string, customScalarAdapters)
}