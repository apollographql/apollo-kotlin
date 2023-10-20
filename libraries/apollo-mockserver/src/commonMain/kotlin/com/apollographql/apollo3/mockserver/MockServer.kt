package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.Closeable
import kotlin.jvm.JvmOverloads

interface MockServer : Closeable {
  /**
   * Returns the root url for this server
   *
   * It will suspend until a port is found to listen to
   */
  suspend fun url(): String

  @Deprecated("use close instead", ReplaceWith("close"), DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  suspend fun stop() = close()

  /**
   * Closes the server. Might be asynchronous on some platforms.
   */
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

internal class MockServerImpl(override val mockServerHandler: MockServerHandler, acceptDelayMillis: Int) : MockServer {
  private val server = SocketServer(acceptDelayMillis)
  private val requests = mutableListOf<MockRequest>()
  private val lock = reentrantLock()
  private val scope = CoroutineScope(SupervisorJob())

  init {
    server.start(::onSocket)
  }

  private fun onSocket(socket: Socket) {
    scope.launch {
      //println("Socket bound: ${url()}")
      try {
        handleRequests(mockServerHandler, socket) {
          lock.withLock {
            requests.add(it)
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) {
          // We got cancelled from closing the server
          throw e
        }

        println("handling request failed")
        // There was a network exception
        e.printStackTrace()
      } finally {
        socket.close()
      }
    }
  }

  private suspend fun handleRequests(handler: MockServerHandler, socket: Socket, onRequest: (MockRequest) -> Unit) {
    val buffer = Buffer()
    while (true) {
      val request = readRequest(
          object : Reader {
            override val buffer: Buffer
              get() = buffer

            override suspend fun fillBuffer() {
              val data = socket.receive()
              buffer.write(data)
            }
          }
      )
      onRequest(request)

      val response = handler.handle(request)

      delay(response.delayMillis)

      writeResponse(response, request.version) {
        socket.write(it)
      }
    }
  }

  override suspend fun url(): String {
    return server.address().let {
      // XXX: IPv6
      "http://127.0.0.1:${it.port}/"
    }
  }

  override fun close() {
    scope.cancel()
    server.close()
  }

  override fun enqueue(mockResponse: MockResponse) {
    (mockServerHandler as? QueueMockServerHandler)?.enqueue(mockResponse)
        ?: error("Apollo: cannot call MockServer.enqueue() with a custom handler")
  }

  override fun takeRequest(): MockRequest {
    return lock.withLock {
      requests.removeFirst()
    }
  }

}

@JvmOverloads
fun MockServer(mockServerHandler: MockServerHandler = QueueMockServerHandler(), acceptDelayMillis: Int = 0): MockServer = MockServerImpl(mockServerHandler, acceptDelayMillis)

@Deprecated("Use enqueueString instead", ReplaceWith("enqueueString"), DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun MockServer.enqueue(string: String = "", delayMs: Long = 0, statusCode: Int = 200) = enqueueString(string, delayMs, statusCode)

fun MockServer.enqueueString(string: String = "", delayMs: Long = 0, statusCode: Int = 200) {
  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body(string)
      .delayMillis(delayMs)
      .build())
}

@ApolloExperimental
interface MultipartBody {
  fun enqueuePart(bytes: ByteString, isLast: Boolean)
  fun enqueueDelay(delayMillis: Long)
}

@ApolloExperimental
fun MultipartBody.enqueueStrings(parts: List<String>, responseDelayMillis: Long = 0, chunksDelayMillis: Long = 0) {
  enqueueDelay(responseDelayMillis)
  parts.withIndex().forEach { (index, value) ->
    enqueueDelay(chunksDelayMillis)
    enqueuePart(value.encodeUtf8(), index == parts.lastIndex)
  }
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
          .addHeader("Content-Type", """multipart/mixed; boundary="$boundary""")
          .addHeader("Transfer-Encoding", "chunked")
          .build()
  )

  return multipartBody
}