package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloExperimental
import okio.Closeable

interface MockServer: Closeable {
  /**
   * Returns the root url for this server
   *
   * It will suspend until a port is found to listen to
   */
  suspend fun url(): String

  /**
   * Stops the server
   */
  @Deprecated("Use close instead", ReplaceWith("close"), DeprecationLevel.ERROR)
  suspend fun stop()

  override fun close()

  /**
   * The mock server handler used to respond to requests.
   *
   * The default handler is a [QueueMockServerHandler], which serves a fixed sequence of responses from a queue (see [enqueue]).
   */
  val mockServerHandler: MockServerHandler

  /**
   * Enqueue a response
   */
  fun enqueue(mockResponse: MockResponse)

  /**
   * Returns a request from the recorded requests or throws if no request has been received
   */
  fun takeRequest(): MockRequest
}

expect fun MockServer(mockServerHandler: MockServerHandler = QueueMockServerHandler()): MockServer

fun MockServer.enqueue(string: String = "", delayMs: Long = 0, statusCode: Int = 200) {
  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body(string)
      .delayMillis(delayMs)
      .build())
}

@ApolloExperimental
fun MockServer.enqueueMultipart(
    parts: List<String>,
    statusCode: Int = 200,
    partsContentType: String = "application/json; charset=utf-8",
    headers: Map<String, String> = emptyMap(),
    responseDelayMillis: Long = 0,
    chunksDelayMillis: Long = 0,
    boundary: String = "-",
) {
  enqueue(createMultipartMixedChunkedResponse(
      parts = parts,
      statusCode = statusCode,
      partsContentType = partsContentType,
      headers = headers,
      responseDelayMillis = responseDelayMillis,
      chunksDelayMillis = chunksDelayMillis,
      boundary = boundary
  ))
}