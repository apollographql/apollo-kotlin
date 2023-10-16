package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloExperimental
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.Closeable

interface MockServer : Closeable {
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
   * The default handler is a [QueueMockServerHandler], which serves a fixed sequence of responses from a queue (see [enqueueString]).
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

@Deprecated("Use enqueueString instead", ReplaceWith("enqueueString"), DeprecationLevel.ERROR)
fun MockServer.enqueue(string: String = "", delayMs: Long = 0, statusCode: Int = 200) = enqueueString(string, delayMs, statusCode)

fun MockServer.enqueueString(string: String = "", delayMs: Long = 0, statusCode: Int = 200) {
  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body(string)
      .delayMillis(delayMs)
      .build())
}

@ApolloExperimental
interface MultipartBody : Closeable {
  fun enqueuePart(bytes: ByteString)
  fun enqueueDelay(delayMillis: Long)
  override fun close()
}

@ApolloExperimental
fun MultipartBody.enqueueStrings(parts: List<String>, responseDelayMillis: Long = 0, chunksDelayMillis: Long = 0) {
  enqueueDelay(responseDelayMillis)
  parts.forEach {
    enqueueDelay(chunksDelayMillis)
    enqueuePart(it.encodeUtf8())
  }
  close()
}

@ApolloExperimental
fun MockServer.enqueueMultipart(
    partsContentType: String,
    headers: Map<String, String> = emptyMap(),
    boundary: String = "-",
): MultipartBody {
  val multipartBody = MultipartBodyImpl(boundary, partsContentType)
  enqueue(
      MockResponse.Builder()
          .body(multipartBody.consumeAsFlow())
          .headers(headers)
          .addHeader("Content-Type", """"multipart/mixed; boundary="$boundary"""")
          .addHeader("Transfer-Encoding", "chunked")
          .build()
  )

  return multipartBody
}