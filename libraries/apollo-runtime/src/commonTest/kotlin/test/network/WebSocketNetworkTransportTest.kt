package test.network

import app.cash.turbine.test
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloWebSocketClosedException
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.exception.SubscriptionOperationException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo.network.websocket.closeConnection
import test.FooSubscription
import test.FooSubscription.Companion.completeMessage
import test.FooSubscription.Companion.errorMessage
import test.FooSubscription.Companion.nextMessage
import test.network.connectionAckMessage
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.CloseFrame
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.TextMessage
import com.apollographql.mockserver.WebSocketBody
import com.apollographql.mockserver.WebsocketMockRequest
import com.apollographql.mockserver.awaitWebSocketRequest
import com.apollographql.mockserver.enqueueWebSocket
import com.apollographql.mockserver.headerValueOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class WebSocketNetworkTransportTest {
  @Test
  fun simple() = mockServerWebSocketTest {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()

          assertEquals("graphql-transport-ws", serverReader.headers.headerValueOf("sec-websocket-protocol"))

          val operationId = serverReader.awaitSubscribe()
          repeat(5) {
            serverWriter.enqueueMessage(nextMessage(operationId, it))
            assertEquals(it, awaitItem().data?.foo)
          }
          serverWriter.enqueueMessage(completeMessage(operationId))

          awaitComplete()
        }
  }

  @Test
  fun interleavedSubscriptions() = mockServerWebSocketTest {
    0.until(2).map {
      apolloClient.subscription(FooSubscription()).toFlow()
    }.merge()
        .test {
          awaitConnectionInit()
          val operationId1 = serverReader.awaitSubscribe()
          val operationId2 = serverReader.awaitSubscribe()

          repeat(3) {
            serverWriter.enqueueMessage(nextMessage(operationId1, it))
            awaitItem().apply {
              assertEquals(operationId1, requestUuid.toString())
              assertEquals(it, data?.foo)
            }
            serverWriter.enqueueMessage(nextMessage(operationId2, it))
            awaitItem().apply {
              assertEquals(operationId2, requestUuid.toString())
              assertEquals(it, data?.foo)
            }
          }

          serverWriter.enqueueMessage(completeMessage(operationId1))
          serverWriter.enqueueMessage(completeMessage(operationId2))

          awaitComplete()
        }
  }

  @Test
  fun slowConsumer() = mockServerWebSocketTest {
    /**
     * Simulate a low read on the first 5 items.
     * During that time, the server should continue sending.
     * Then resume reading as fast as possible and make sure we didn't drop any items.
     */
    /**
     * Simulate a low read on the first 5 items.
     * During that time, the server should continue sending.
     * Then resume reading as fast as possible and make sure we didn't drop any items.
     */
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()
          val operationId = serverReader.awaitSubscribe()
          repeat(1000) {
            serverWriter.enqueueMessage(nextMessage(operationId, it))
          }
          delay(3000)
          repeat(1000) {
            assertEquals(it, awaitItem().data?.foo)
          }
          serverWriter.enqueueMessage(completeMessage(operationId))

          awaitComplete()
        }
  }

  @Test
  fun operationError() = mockServerWebSocketTest {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()

          val operationId = serverReader.awaitSubscribe()
          serverWriter.enqueueMessage(errorMessage(operationId, "Woops"))
          awaitItem().exception.apply {
            assertIs<SubscriptionOperationException>(this)
            assertEquals("Woops", ((payload as List<*>).first() as Map<*, *>).get("message"))
          }
          awaitComplete()
        }
  }

  @Test
  fun unknownMessagesDoNotStopTheFlow() = runTest {
    MockServer().use { mockServer ->
      val serverWriter = mockServer.enqueueWebSocket()

      ApolloClient.Builder()
          .serverUrl("unused")
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(mockServer.url())
                  .build()
          )
          .build()
          .use { apolloClient ->
            apolloClient.subscription(FooSubscription())
                .toFlow()
                .test {
                  val serverReader = mockServer.awaitWebSocketRequest()
                  serverReader.awaitMessage() // Consume connection_init

                  serverWriter.enqueueMessage(connectionAckMessage())
                  val operationId = serverReader.awaitSubscribe()

                  serverWriter.enqueueMessage(nextMessage(operationId, 42))
                  assertEquals(42, awaitItem().data?.foo)

                  serverWriter.enqueueMessage(TextMessage("\"Ignored Message\""))
                  expectNoEvents()

                  serverWriter.enqueueMessage(nextMessage(operationId, 41))
                  assertEquals(41, awaitItem().data?.foo)

                  serverWriter.enqueueMessage(completeMessage(operationId))
                  awaitComplete()
                }
          }
    }
  }

  @Test
  fun connectionErrorEmitsException() = mockServerWebSocketTest(
      customizeTransport = {
        connectionAcknowledgeTimeout(500.milliseconds)
      }
  ) {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitWebSocketRequest()
          serverReader.awaitMessage() // connection_init

          // no connection_ack here => timeout
          awaitItem().exception.apply {
            assertIs<ApolloNetworkException>(this)
            assertEquals("Timeout while waiting for connection_ack", message)
          }

          awaitComplete()
        }
  }

  @Test
  fun socketClosedEmitsException() = runTest {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(mockServer.url())
                  .build()
          )
          .build()
          .use { apolloClient ->
            apolloClient.subscription(FooSubscription())
                .toFlow()
                .test {
                  val webSocketBody = mockServer.enqueueWebSocket()
                  val webSocketMockRequest = mockServer.awaitWebSocketRequest()
                  webSocketMockRequest.awaitMessage() // connection_init

                  webSocketBody.enqueueMessage(CloseFrame(3666, "closed"))

                  awaitItem().exception.apply {
                    @Suppress("DEPRECATION")
                    when (com.apollographql.apollo.testing.platform()) {
                      com.apollographql.apollo.testing.Platform.Native -> {
                        assertIs<DefaultApolloException>(this)
                        assertTrue(message?.contains("Error reading websocket") == true)
                      }

                      else -> {
                        assertIs<ApolloWebSocketClosedException>(this)
                        assertEquals(3666, code)
                        assertEquals("closed", reason)
                      }
                    }
                  }

                  awaitComplete()
                }
          }
    }
  }


  @Test
  fun disposingTheClientClosesTheWebSocket() = mockServerWebSocketTest {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()
          val operationId = serverReader.awaitSubscribe()
          serverWriter.enqueueMessage(nextMessage(operationId, 0))
          awaitItem()
          serverWriter.enqueueMessage(completeMessage(operationId))
          serverReader.awaitMessage() // consume complete
          awaitComplete()
        }

    apolloClient.close()

    delay(100)

    val result = serverReader.awaitMessageOrClose()
    if (result.isSuccess) {
      val clientClose = result.getOrThrow()
      assertIs<CloseFrame>(clientClose)
      /**
       * OkHttp keeps the TCP connection alive, do not await a close of the message flow
       */
//      serverWriter.enqueueMessage(CloseFrame(clientClose.code, clientClose.reason))
//      serverReader.awaitClose(5.minutes)
    }
  }

  @Test
  fun flowThrowsIfNoReconnect() = mockServerWebSocketTest {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()
          val operationId = serverReader.awaitSubscribe()
          serverWriter.enqueueMessage(nextMessage(operationId, 0))
          awaitItem()
          serverWriter.enqueueMessage(CloseFrame(1001, "flowThrowsIfNoReconnect"))
          awaitItem().exception.apply {
            @Suppress("DEPRECATION")
            when (com.apollographql.apollo.testing.platform()) {
              com.apollographql.apollo.testing.Platform.Native -> {
                assertIs<DefaultApolloException>(this)
                assertTrue(message?.contains("Error reading websocket") == true)
              }

              else -> {
                assertIs<ApolloWebSocketClosedException>(this)
                assertEquals(1001, code)
                assertEquals("flowThrowsIfNoReconnect", reason)
              }
            }
          }
          awaitComplete()
        }
  }

  @Test
  fun closeConnectionTest() = runTest {
    MockServer().use { mockServer ->
      var exception: ApolloException? = null

      ApolloClient.Builder()
          .httpServerUrl(mockServer.url())
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(mockServer.url())
                  .build()
          )
          .retryWhen { e, _ ->
            check(exception == null)
            exception = e
            true
          }
          .build()
          .use { apolloClient ->
            var serverWriter = mockServer.enqueueWebSocket()
            apolloClient.subscription(FooSubscription())
                .toFlow()
                .test {
                  var serverReader = mockServer.awaitWebSocketRequest()

                  serverReader.awaitMessage()
                  serverWriter.enqueueMessage(connectionAckMessage())

                  var operationId = serverReader.awaitSubscribe()
                  serverWriter.enqueueMessage(nextMessage(operationId, 0))

                  assertEquals(0, awaitItem().data?.foo)

                  /**
                   * Prepare next websocket
                   */
                  serverWriter = mockServer.enqueueWebSocket()

                  /**
                   * call closeConnection
                   */
                  apolloClient.subscriptionNetworkTransport.closeConnection(DefaultApolloException("oh no!"))

                  serverReader = mockServer.awaitWebSocketRequest()

                  serverReader.awaitMessage()
                  serverWriter.enqueueMessage(connectionAckMessage())

                  operationId = serverReader.awaitSubscribe()
                  serverWriter.enqueueMessage(nextMessage(operationId, 1))

                  assertEquals(1, awaitItem().data?.foo)

                  serverWriter.enqueueMessage(completeMessage(operationId))

                  awaitComplete()
                }
          }

      exception.apply {
        assertIs<DefaultApolloException>(this)
        assertEquals("oh no!", message)
      }
    }
  }

  @Test
  fun canChangeHeadersInInterceptor() = runTest {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .httpServerUrl(mockServer.url())
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(mockServer.url())
                  .build()
          )
          .addInterceptor(MyInterceptor())
          .build()
          .use { apolloClient ->
            val serverWriter0 = mockServer.enqueueWebSocket()
            apolloClient.subscription(FooSubscription())
                .toFlow()
                .test(timeout = 300.seconds) {
                  val serverReader0 = mockServer.awaitWebSocketRequest()
                  assertEquals("0", serverReader0.headers.headerValueOf("authorization"))
                  serverReader0.awaitMessage()
                  serverWriter0.enqueueMessage(connectionAckMessage())
                  val id0 = serverReader0.awaitSubscribe()
                  serverWriter0.enqueueMessage(nextMessage(id0, 0))

                  awaitItem().apply {
                    assertEquals(0, data?.foo)
                  }

                  // Prepare the next websocket
                  val serverWriter1 = mockServer.enqueueWebSocket()

                  // Send an error to the interceptor, should retry the chain under the hood
                  serverWriter0.enqueueMessage(nextMessage(id0, "unauthorized"))

                  val serverReader1 = mockServer.awaitWebSocketRequest()
                  assertEquals("1", serverReader1.headers.headerValueOf("authorization"))
                  serverReader1.awaitMessage()
                  serverWriter1.enqueueMessage(connectionAckMessage())
                  val id1 = serverReader1.awaitSubscribe()
                  serverWriter1.enqueueMessage(nextMessage(id1, 1))

                  awaitItem().apply {
                    assertEquals(1, data?.foo)
                  }

                  serverWriter1.enqueueMessage(completeMessage(id1))
                  awaitComplete()
                }
          }
    }
  }

  @Test
  fun idleTimeoutIsObserved() = mockServerWebSocketTest(
      customizeTransport = { idleTimeout(2.seconds) }
  ) {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()

          val operationId = serverReader.awaitSubscribe()
          serverWriter.enqueueMessage(nextMessage(operationId, 0))
          assertEquals(0, awaitItem().data?.foo)
          serverWriter.enqueueMessage(completeMessage(operationId))
          awaitComplete()
        }

    // Consume the stopOperation()
    serverReader.awaitComplete()

    // And wait for a close frame
    val time = measureTime {
      serverReader.awaitMessage(3.seconds).apply {
        assertIs<CloseFrame>(this)
      }
    }

    // Make sure that we have waited a bit before closing the websocket
    assertTrue(time > 1.5.seconds)
  }

  private object RetryException : Exception()

  private class MyInterceptor : ApolloInterceptor {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
      var counter = -1

      return flow {
        counter++
        emit(
            request.newBuilder()
                .addHttpHeader("Authorization", counter.toString())
                .build()
        )
      }.flatMapConcat {
        chain.proceed(it)
      }.onEach {
        if (it.errors.orEmpty().isNotEmpty()) {
          throw RetryException
        }
      }.retryWhen { cause, _ ->
        cause is RetryException
      }
    }
  }
}

internal fun WebSocketBody.enqueueMessage(message: String) {
  enqueueMessage(TextMessage(message))
}

class MockServerWebSocketTest(
    val apolloClient: ApolloClient,
    private val mockServer: MockServer,
    val coroutineScope: CoroutineScope,
) {
  /**
   * Enqueue the response straight away
   */
  val serverWriter: WebSocketBody = mockServer.enqueueWebSocket()
  private var _serverReader: WebsocketMockRequest? = null

  val serverReader: WebsocketMockRequest
    get() {
      check(_serverReader != null) {
        "You need to call awaitConnectionInit or awaitWebSocketRequest first"
      }
      return _serverReader!!
    }

  suspend fun awaitWebSocketRequest() {
    _serverReader = mockServer.awaitWebSocketRequest()
  }

  suspend fun awaitConnectionInit() {
    awaitWebSocketRequest()

    serverReader.awaitMessage()
    serverWriter.enqueueMessage(TextMessage(connectionAckMessage()))
  }
}

fun mockServerWebSocketTest(
    customizeTransport: WebSocketNetworkTransport.Builder.() -> Unit = {},
    block: suspend MockServerWebSocketTest.() -> Unit,
) = runTest {
  MockServer().use { mockServer ->

    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .apply(customizeTransport)
                .build()
        )
        .build().use { apolloClient ->
          MockServerWebSocketTest(apolloClient, mockServer, this@runTest).block()
        }
  }
}

private object RetryException : Exception()

private fun <D : Operation.Data> Flow<ApolloResponse<D>>.retryOnError(block: suspend (ApolloException, Int) -> Boolean): Flow<ApolloResponse<D>> {
  var attempt = 0
  return onEach {
    if (it.exception != null && block(it.exception!!, attempt)) {
      attempt++
      throw RetryException
    }
  }.retryWhen { cause, _ ->
    cause is RetryException
  }
}

internal class RetryOnErrorInterceptor(private val retryWhen: suspend (ApolloException, Int) -> Boolean) : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(request).retryOnError(retryWhen)
  }
}

internal fun ApolloClient.Builder.retryWhen(retryWhen: suspend (ApolloException, Int) -> Boolean) = apply {
  retryOnErrorInterceptor(RetryOnErrorInterceptor(retryWhen))
}