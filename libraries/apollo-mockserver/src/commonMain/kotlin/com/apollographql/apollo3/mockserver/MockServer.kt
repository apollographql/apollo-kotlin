package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.Closeable
import kotlin.js.JsName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A server for testing Kotlin Multiplatform applications using HTTP and WebSockets.
 *
 * A [MockServer] binds to localhost and allows to enqueue predefined responses using [enqueue], [enqueueString],
 * [enqueueMultipart]  and [enqueueWebSocket]
 *
 * [MockServer] is very simple and should not be used for production applications. HTTPS is a non-goal as well as
 * performance.
 * Also, [MockServer] makes no attempt at flow control:
 * - data is read as fast as possible from the network and buffered until [takeRequest] or [awaitAnyRequest] is called.
 * - queued responses from [enqueue] are buffered until they can be transmitted to the network.
 * If you're using [MockServer] to handle large payloads, it will use a lot of memory.
 */
interface MockServer : Closeable {
  /**
   * Returns the url for this server in the form "http://ip:port/".
   *
   * This function is suspend because finding an available port is an asynchronous operation on some platforms.
   */
  suspend fun url(): String

  /**
   * Returns the port used by this server.
   *
   * This function is suspend because finding an available port is an asynchronous operation on some platforms.
   */
  suspend fun port(): Int

  @Deprecated("use close instead", ReplaceWith("close()"), DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  suspend fun stop() = close()

  /**
   * Closes the server.
   *
   * The locally bound socket listening to new connections is freed immediately.
   * Active connections might stay alive after this call but will eventually terminate.
   */
  override fun close()

  /**
   * Enqueue a response.
   */
  fun enqueue(mockResponse: MockResponse)

  /**
   * Return a request from the recorded requests or throw if no request has been received.
   *
   * @see [awaitRequest] and [awaitWebSocketRequest]
   */
  fun takeRequest(): MockRequest

  /**
   * Wait for a request and return it.
   *
   * @see [awaitRequest] and [awaitWebSocketRequest]
   */
  suspend fun awaitAnyRequest(timeout: Duration = 1.seconds): MockRequestBase

  interface Listener {
    fun onRequest(request: MockRequestBase)
    fun onMessage(message: WebSocketMessage)
  }

  class Builder {
    private var handler: MockServerHandler? = null
    private var handlePings: Boolean? = null
    private var tcpServer: TcpServer? = null
    private var listener: Listener? = null
    private var port: Int? = null

    fun handler(handler: MockServerHandler) = apply {
      this.handler = handler
    }

    fun handlePings(handlePings: Boolean) = apply {
      this.handlePings = handlePings
    }

    fun tcpServer(tcpServer: TcpServer) = apply {
      this.tcpServer = tcpServer
    }

    fun port(port: Int) = apply {
      this.port = port
    }

    fun listener(listener: Listener) = apply {
      this.listener = listener
    }

    fun build(): MockServer {
      check(tcpServer == null || port == null) {
        "It is an error to set both tcpServer and port"
      }
      val server = tcpServer ?: TcpServer(port ?: 0)
      return MockServerImpl(
          handler ?: QueueMockServerHandler(),
          handlePings ?: true,
          server,
          listener
      )
    }
  }
}

internal class MockServerImpl(
    private val mockServerHandler: MockServerHandler,
    private val handlePings: Boolean,
    private val server: TcpServer,
    private val listener: MockServer.Listener?,
) : MockServer {
  private val requests = Channel<MockRequestBase>(Channel.UNLIMITED)
  private val scope = CoroutineScope(SupervisorJob())

  init {
    server.listen(::onSocket)
  }

  private fun onSocket(socket: TcpSocket) {
    scope.launch {
      try {
        handleRequests(mockServerHandler, socket, listener) {
          requests.trySend(it)
        }
      } catch (e: Exception) {
        when (e) {
          is CancellationException -> {
            // We got cancelled from closing the server => propagate the exception
            throw e
          }

          is ConnectionClosed -> {
            // Nothing, ignore those
          }

          else -> {
            println("handling request failed")
            // There was a network exception while reading a request
            e.printStackTrace()
          }
        }
      } finally {
        socket.close()
      }
    }
  }

  private suspend fun handleRequests(
      handler: MockServerHandler,
      socket: TcpSocket,
      listener: MockServer.Listener?,
      onRequest: (MockRequestBase) -> Unit,
  ) {
    val buffer = Buffer()
    val reader = object : Reader {
      override val buffer: Buffer
        get() = buffer

      override suspend fun fillBuffer() {
        val data = socket.receive()
        buffer.write(data)
      }
    }

    var done = false
    while (!done) {
      val request = readRequest(reader)
      listener?.onRequest(request)

      onRequest(request)

      val response = handler.handle(request)

      delay(response.delayMillis)

      coroutineScope {
        if (request is WebsocketMockRequest) {
          launch {
            try {
              readFrames(reader) { message ->
                listener?.onMessage(message)
                when {
                  handlePings && message is PingFrame -> {
                    socket.send(pongFrame())
                  }

                  handlePings && message is PongFrame -> {
                    // do nothing
                  }

                  else -> {
                    request.messages.trySend(Result.success(message))
                  }
                }
              }
            } catch (e: Exception) {
              request.messages.trySend(Result.failure(e))
              throw e
            }
          }
        }
        writeResponse(response, request.version) {
          socket.send(it)
        }
        if (!response.keepAlive) {
          done = true
          cancel()
        }
      }
    }
  }

  override suspend fun port(): Int {
    return server.address().port
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
    val result = requests.tryReceive()

    return result.getOrThrow() as MockRequest
  }

  override suspend fun awaitAnyRequest(timeout: Duration): MockRequestBase {
    return withTimeout(timeout) {
      requests.receive()
    }
  }
}

@JsName("createMockServer")
fun MockServer(): MockServer = MockServerImpl(
    QueueMockServerHandler(),
    true,
    TcpServer(0),
    null
)

@Deprecated("Use MockServer.Builder() instead", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun MockServer(handler: MockServerHandler): MockServer =
  MockServerImpl(
      handler,
      true,
      TcpServer(0),
      null
  )

@Deprecated("Use enqueueString instead", ReplaceWith("enqueueString(string = string, delayMs = delayMs, statusCode = statusCode)"), DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun MockServer.enqueue(string: String = "", delayMs: Long = 0, statusCode: Int = 200) = enqueueString(string, delayMs, statusCode)

fun MockServer.enqueueString(string: String = "", delayMs: Long = 0, statusCode: Int = 200, contentType: String = "text/plain") {
  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body(string)
      .addHeader("Content-Type", contentType)
      .delayMillis(delayMs)
      .build()
  )
}

fun MockServer.enqueueError(statusCode: Int) {
  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body("")
      .addHeader("Content-Type", "text/plain")
      .build()
  )
}

fun MockServer.enqueueGraphQLString(
    string: String,
    delayMs: Long = 0,
) = enqueueString(
    string = string,
    delayMs = delayMs,
    contentType = "application/graphql-response+json"
)

fun MockServer.assertNoRequest() {
  try {
    takeRequest()
    error("Apollo: response(s) were received")
  } catch (_: Exception) {

  }
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

@Deprecated("Use enqueueStrings()", ReplaceWith("enqueueMultipart(\"application/json\").enqueueStrings(parts)"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Suppress("UNUSED_PARAMETER")
fun MockServer.enqueueMultipart(
    parts: List<String>,
): Nothing = TODO()

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

@ApolloExperimental
interface WebSocketBody {
  fun enqueueMessage(message: WebSocketMessage)
  fun close()
}

@ApolloExperimental
fun MockServer.enqueueWebSocket(
    statusCode: Int = 101,
    headers: Map<String, String> = emptyMap(),
    keepAlive: Boolean = true,
): WebSocketBody {
  val webSocketBody = WebSocketBodyImpl()
  enqueue(
      MockResponse.Builder()
          .statusCode(statusCode)
          .body(webSocketBody.consumeAsFlow())
          .headers(headers)
          .keepAlive(keepAlive)
          .addHeader("Upgrade", "websocket")
          .addHeader("Connection", "upgrade")
          .addHeader("Sec-WebSocket-Accept", "APOLLO_REPLACE_ME")
          .addHeader("Sec-WebSocket-Protocol", "APOLLO_REPLACE_ME")
          .build()
  )

  return webSocketBody
}

@ApolloExperimental
suspend fun MockServer.awaitWebSocketRequest(timeout: Duration = 30.seconds): WebsocketMockRequest {
  return awaitAnyRequest(timeout) as WebsocketMockRequest
}

suspend fun MockServer.awaitRequest(timeout: Duration = 30.seconds): MockRequest {
  return awaitAnyRequest(timeout) as MockRequest
}