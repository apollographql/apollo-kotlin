@file:JvmName("Operations")
package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.http.ApolloHttpRequestComposer
import com.apollographql.apollo3.api.internal.ResponseBodyParser
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.writeObject
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import kotlin.jvm.JvmName


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
    customScalarAdapters: CustomScalarAdapters,
): ApolloResponse<D> {
  return ResponseBodyParser.parse(source, this, customScalarAdapters)
}

/**
 * See [parseJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    source: BufferedSource,
): ApolloResponse<D>  = parseJsonResponse(source, CustomScalarAdapters.Empty)

/**
 * See [parseJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    byteString: ByteString,
    customScalarAdapters: CustomScalarAdapters,
): ApolloResponse<D> {
  return parseJsonResponse(Buffer().write(byteString), customScalarAdapters)
}

/**
 * See [parseJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    byteString: ByteString,
): ApolloResponse<D> {
  return parseJsonResponse(byteString, CustomScalarAdapters.Empty)
}

/**
 * See [parseJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    string: String,
    customScalarAdapters: CustomScalarAdapters,
): ApolloResponse<D> {
  return parseJsonResponse(Buffer().writeUtf8(string), customScalarAdapters)
}

/**
 * See [parseJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.parseJsonResponse(
    string: String,
): ApolloResponse<D> {
  return parseJsonResponse(Buffer().writeUtf8(string), CustomScalarAdapters.Empty)
}

/**
 * Reads only the "data" part of a GraphQL Json response
 */
fun <D : Operation.Data> Operation<D>.parseJsonData(
    source: BufferedSource,
    customScalarAdapters: CustomScalarAdapters,
): D {
  return adapter().fromJson(source, customScalarAdapters)
}

/**
 * See [parseJsonData]
 */
fun <D : Operation.Data> Operation<D>.parseJsonData(
    source: BufferedSource,
): D {
  return adapter().fromJson(source, CustomScalarAdapters.Empty)
}

/**
 * see [parseJsonData]
 */
fun <D : Operation.Data> Operation<D>.parseJsonData(
    byteString: ByteString,
    customScalarAdapters: CustomScalarAdapters,
): D {
  return adapter().fromJson(Buffer().write(byteString), customScalarAdapters)
}

/**
 * see [parseJsonData]
 */
fun <D : Operation.Data> Operation<D>.parseJsonData(
    byteString: ByteString,
): D {
  return adapter().fromJson(Buffer().write(byteString), CustomScalarAdapters.Empty)
}

/**
 * see [parseJsonData]
 */
fun <D : Operation.Data> Operation<D>.parseJsonData(
    string: String,
    customScalarAdapters: CustomScalarAdapters,
): D {
  return adapter().fromJson(string, customScalarAdapters)
}

/**
 * see [parseJsonData]
 */
fun <D : Operation.Data> Operation<D>.parseJsonData(
    string: String,
): D {
  return adapter().fromJson(string, CustomScalarAdapters.Empty)
}

/**
 * writes a successful GraphQL Json response containing "data" to the given sink.
 *
 * Use this for testing/mocking a valid GraphQL response
 */
fun <D : Operation.Data> Operation<D>.composeJsonResponse(
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
 * See [composeJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.composeJsonResponse(
    sink: BufferedSink,
    data: D,
): Unit = composeJsonResponse(sink, data, CustomScalarAdapters.Empty, "  ")

/**
 * see [composeJsonResponse]
 */
fun <D : Operation.Data> Operation<D>.composeJsonResponse(
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
fun <D : Operation.Data> Operation<D>.composeJsonResponse(
    data: D,

): String = composeJsonResponse(data, CustomScalarAdapters.Empty, "  ")

/**
 * writes operation data to the given sink
 *
 * Use this for testing/mocking a valid GraphQL response
 */
fun <D : Operation.Data> Operation<D>.composeJsonData(
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
fun <D : Operation.Data> Operation<D>.composeJsonData(
    sink: BufferedSink,
    data: D,
): Unit = composeJsonData(sink, data, CustomScalarAdapters.Empty, "  ")

/**
 * See [composeJsonData]
 */
fun <D : Operation.Data> Operation<D>.composeJsonData(
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

fun <D : Operation.Data> Operation<D>.composeJsonData(
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
fun <D : Operation.Data> Operation<D>.composeJsonRequest(
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
fun <D : Operation.Data> Operation<D>.composeJsonRequest(
    sink: BufferedSink,
): Unit = composeJsonRequest(sink, CustomScalarAdapters.Empty)

/**
 * see [composeJsonRequest]
 */
fun <D : Operation.Data> Operation<D>.composeJsonRequest(
    customScalarAdapters: CustomScalarAdapters,
): String {
  return Buffer().apply {
    composeJsonRequest(this, customScalarAdapters)
  }.readUtf8()
}


/**
 * see [composeJsonRequest]
 */
fun <D : Operation.Data> Operation<D>.composeJsonRequest(): String = composeJsonRequest(CustomScalarAdapters.Empty)