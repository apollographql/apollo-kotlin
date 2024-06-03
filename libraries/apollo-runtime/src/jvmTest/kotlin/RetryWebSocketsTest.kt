
import app.cash.turbine.test
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.interceptor.addRetryOnErrorInterceptor
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.FooQuery
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.FooSubscription.Companion.completeMessage
import com.apollographql.apollo3.testing.FooSubscription.Companion.nextMessage
import com.apollographql.apollo3.testing.awaitSubscribe
import com.apollographql.apollo3.testing.connectionAckMessage
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okio.use
import test.network.enqueueMessage
import test.network.mockServerTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.seconds

class RetryWebSocketsTest {
  @Test
  fun retryIsWorking() = runTest(skipDelays = false) {
    MockServer().use { mockServer ->

    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .retryOnError { it.operation is Subscription }
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .build().use { apolloClient ->
          apolloClient.subscription(FooSubscription())
              .toFlow()
              .test {
                val serverWriter = mockServer.enqueueWebSocket(keepAlive = false)
                var serverReader = mockServer.awaitWebSocketRequest()

                serverReader.awaitMessage() // connection_init
                serverWriter.enqueueMessage(connectionAckMessage())

                val operationId1 = serverReader.awaitSubscribe()
                serverWriter.enqueueMessage(nextMessage(operationId1, 1))

                val item1 = awaitItem()
                assertEquals(1, item1.data?.foo)

                val serverWriter2 = mockServer.enqueueWebSocket()

                /**
                 * Close the response body and the TCP socket
                 */
                serverWriter.close()

                serverReader = mockServer.awaitWebSocketRequest()

                serverReader.awaitMessage() // connection_init
                serverWriter2.enqueueMessage(connectionAckMessage())

                val operationId2 = serverReader.awaitSubscribe()
                serverWriter2.enqueueMessage(nextMessage(operationId2, 2))

                val item2 = awaitItem()
                assertEquals(2, item2.data?.foo)

                // The subscriptions MUST use different operationIds
                assertNotEquals(operationId1, operationId2)

                serverWriter2.enqueueMessage(completeMessage(operationId2))

                awaitComplete()
              }
        }
    }
  }

  @Test
  fun socketReopensAfterAnError() = runTest(false) {
    var mockServer = MockServer()

    ApolloClient.Builder()
        .httpServerUrl(mockServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .addRetryOnErrorInterceptor { _, _ ->
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
                 * Close the server, the flow should restart
                 */
                val port = mockServer.port()
                mockServer.close()

                /**
                 * Looks like Ktor needs some time to close the local address.
                 * Without the delay, I'm getting an "address already in use" error
                 */
                delay(1000)
                mockServer = MockServer.Builder().port(port).build()
                serverWriter = mockServer.enqueueWebSocket()

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
  }

  @Test
  fun retryCanBeDisabled() = runTest(skipDelays = false) {
    val mockServer = MockServer()
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .retryOnError { it.operation is Subscription }
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .build().use { apolloClient ->
          apolloClient.subscription(FooSubscription())
              .retryOnError(false)
              .toFlow()
              .test {
                val serverWriter = mockServer.enqueueWebSocket()
                val serverReader = mockServer.awaitWebSocketRequest()

                serverReader.awaitMessage() // connection_init
                serverWriter.enqueueMessage(connectionAckMessage())

                val operationId1 = serverReader.awaitSubscribe()
                serverWriter.enqueueMessage(nextMessage(operationId1, 1))

                val item1 = awaitItem()
                assertEquals(1, item1.data?.foo)

                /**
                 * Close the server to trigger an exception
                 */
                mockServer.close()

                assertIs<ApolloNetworkException>(awaitItem().exception)
                awaitComplete()
              }
        }
  }

  @Test
  fun subscriptionsAreNotRetriedByDefault() = runTest(skipDelays = false) {
    val mockServer = MockServer()
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .build().use { apolloClient ->
          apolloClient.subscription(FooSubscription())
              .toFlow()
              .test {
                val serverWriter = mockServer.enqueueWebSocket()
                val serverReader = mockServer.awaitWebSocketRequest()

                serverReader.awaitMessage() // connection_init
                serverWriter.enqueueMessage(connectionAckMessage())

                val operationId1 = serverReader.awaitSubscribe()
                serverWriter.enqueueMessage(nextMessage(operationId1, 1))

                val item1 = awaitItem()
                assertEquals(1, item1.data?.foo)

                /**
                 * Close the server to trigger an exception
                 */
                mockServer.close()

                assertIs<ApolloNetworkException>(awaitItem().exception)
                awaitComplete()
              }
        }
  }

  @Test
  fun queriesAreNotRetriedWhenRetrySubscriptionsIsTrue() = mockServerTest(
      clientBuilder = {
        retryOnError { it.operation is Subscription }
      },
  ) {
    mockServer.enqueue(MockResponse.Builder().statusCode(500).build())

    apolloClient.query(FooQuery())
        .toFlow()
        .test {
          assertIs<ApolloHttpException>(awaitItem().exception)
          awaitComplete()
        }
  }

  @Test
  fun retriesDoNotPile() = runTest {
    var mockServer = MockServer()
    val port = mockServer.port()

    var reopenCount = 0
    val iterations = 50

    ApolloClient.Builder()
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .serverUrl("https://unused.com/")
        .addRetryOnErrorInterceptor { _, _ ->
          reopenCount++
          delay(500)
          true
        }
        .build()
        .use { apolloClient ->
          prepareMockServer(mockServer, iterations)
          val jobs = (1..iterations).map {
            launch {
              /**
               * We're only using the first item of each subscription
               */
              apolloClient.subscription(FooSubscription())
                  .toFlow()
                  /**
                   * Take 2 item:
                   * - first item straight ahead
                   * - second item is after the retry
                   */
                  .take(2)
                  .collect()
            }
          }

          delay(1000)
          /**
           * Close the MockServer, retries start kicking in and must not pile
           */
          mockServer.close()
          /**
           * Wait a bit for retries to happen
           */
          delay(2_000)
          /**
           * Reopen the MockServer, the second item for each subscription should be emitted quickly after recovery.
           */
          mockServer = MockServer.Builder().port(port = port).build()
          prepareMockServer(mockServer, iterations)

          /**
           * I'm putting 20 here to be safe but in practice, this shouldn't take more than ~1s on a M1 laptop, maybe more in CI
           */
          withTimeout(20.seconds) {
            jobs.forEach {
              it.join()
            }
          }
          mockServer.close()
        }
  }

  private fun CoroutineScope.prepareMockServer(mockServer: MockServer, repeat: Int) {
    launch {
      val webSocket = mockServer.enqueueWebSocket()
      val webSocketRequest = mockServer.awaitWebSocketRequest()

      // connection_init
      webSocketRequest.awaitMessage()
      webSocket.enqueueMessage(connectionAckMessage())

      repeat(repeat) {
        val operationId = webSocketRequest.awaitSubscribe(messagesToIgnore = setOf("complete"))
        webSocket.enqueueMessage(nextMessage(operationId, 42))
      }
    }
  }
}