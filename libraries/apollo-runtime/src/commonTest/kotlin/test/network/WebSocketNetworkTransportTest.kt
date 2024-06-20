package test.network

import app.cash.turbine.test
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.exception.DefaultApolloException
import com.apollographql.apollo3.exception.SubscriptionOperationException
import com.apollographql.apollo3.interceptor.addRetryOnErrorInterceptor
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.network.websocket.closeConnection
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.FooSubscription.Companion.completeMessage
import com.apollographql.apollo3.testing.FooSubscription.Companion.errorMessage
import com.apollographql.apollo3.testing.FooSubscription.Companion.nextMessage
import com.apollographql.apollo3.testing.Platform
import com.apollographql.apollo3.testing.connectionAckMessage
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.platform
import com.apollographql.mockserver.CloseFrame
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.TextMessage
import com.apollographql.mockserver.WebSocketBody
import com.apollographql.mockserver.WebsocketMockRequest
import com.apollographql.mockserver.awaitWebSocketRequest
import com.apollographql.mockserver.enqueueWebSocket
import com.apollographql.mockserver.headerValueOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.merge
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
            assertEquals("Woops", ((payload as List<*>).first() as Map<*,*>).get("message"))
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
        connectionAcknowledgeTimeoutMillis(500)
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
  fun socketClosedEmitsException() = runTest() {
    val mockServer = MockServer()
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
                  when (platform()){
                    Platform.Native -> {
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
            when (platform()){
              Platform.Native -> {
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
    val mockServer = MockServer()
    var exception: ApolloException? = null

    ApolloClient.Builder()
        .httpServerUrl(mockServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .addRetryOnErrorInterceptor { e, _ ->
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

fun mockServerWebSocketTest(customizeTransport: WebSocketNetworkTransport.Builder.() -> Unit = {}, block: suspend MockServerWebSocketTest.() -> Unit) = runTest() {
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
          @Suppress("DEPRECATION")
          MockServerWebSocketTest(apolloClient, mockServer, this@runTest).block()
        }
  }
}
