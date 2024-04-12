import app.cash.turbine.test
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.exception.DefaultApolloException
import com.apollographql.apollo3.interceptor.addRetryOnErrorInterceptor
import com.apollographql.apollo3.mockserver.CloseFrame
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.network.websocket.closeConnection
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.connectionAckMessage
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.mockServerWebSocketTest
import com.apollographql.apollo3.testing.operationId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs


class WebSocketErrorsTest {
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
  fun socketClosedEmitsException() = runTest(false) {
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
                  assertIs<ApolloWebSocketClosedException>(this)
                  assertEquals(3666, code)
                  assertEquals("closed", reason)
                }

                awaitComplete()
              }
        }
  }

  @Test
  fun socketReopensAfterAnError(): Unit = runTest(false) {
    var mockServer = MockServer()

    ApolloClient.Builder()
        .httpServerUrl(mockServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .addRetryOnErrorInterceptor { _, attempt ->
          attempt == 0
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

                var operationId = serverReader.awaitMessage().operationId()
                serverWriter.enqueueMessage(FooSubscription.nextMessage(operationId, 0))

                assertEquals(0, awaitItem().data?.foo)

                /**
                 * Close the server, the flow should restart
                 */
                val port = mockServer.port()
                mockServer.close()
                mockServer = MockServer.Builder().port(port).build()

                serverWriter = mockServer.enqueueWebSocket()
                serverReader = mockServer.awaitWebSocketRequest()

                serverReader.awaitMessage()
                serverWriter.enqueueMessage(connectionAckMessage())

                operationId = serverReader.awaitMessage().operationId()
                serverWriter.enqueueMessage(FooSubscription.nextMessage(operationId, 1))

                assertEquals(1, awaitItem().data?.foo)

                serverWriter.enqueueMessage(FooSubscription.completeMessage(operationId))

                awaitComplete()
              }
        }
  }

  @Test
  fun disposingTheClientClosesTheWebSocket(): Unit = mockServerWebSocketTest {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()
          val operationId = serverReader.awaitMessage().operationId()
          serverWriter.enqueueMessage(FooSubscription.nextMessage(operationId, 0))
          awaitItem()
          serverWriter.enqueueMessage(FooSubscription.completeMessage(operationId))
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
          val operationId = serverReader.awaitMessage().operationId()
          serverWriter.enqueueMessage(FooSubscription.nextMessage(operationId, 0))
          awaitItem()
          serverWriter.enqueueMessage(CloseFrame(1001, "flowThrowsIfNoReconnect"))
          awaitItem().exception.apply {
            assertIs<ApolloWebSocketClosedException>(this)
            assertEquals(1001, code)
            assertEquals("flowThrowsIfNoReconnect", reason)
          }
          awaitComplete()
        }
  }

  @Test
  fun closeConnectionTest(): Unit = runBlocking {
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

                var operationId = serverReader.awaitMessage().operationId()
                serverWriter.enqueueMessage(FooSubscription.nextMessage(operationId, 0))

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

                operationId = serverReader.awaitMessage().operationId()
                serverWriter.enqueueMessage(FooSubscription.nextMessage(operationId, 1))

                assertEquals(1, awaitItem().data?.foo)

                serverWriter.enqueueMessage(FooSubscription.completeMessage(operationId))

                awaitComplete()
              }
        }

    exception.apply {
      assertIs<DefaultApolloException>(this)
      assertEquals("oh no!", message)
    }
  }
}
