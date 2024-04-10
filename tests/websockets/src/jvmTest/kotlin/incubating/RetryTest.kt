@file:Suppress("DEPRECATION")

package incubating

import addRetryOnErrorInterceptor
import app.cash.turbine.test
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.websocket.SubscriptionWsProtocol
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.ackMessage
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.mockServerTest
import com.apollographql.apollo3.testing.operationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import sample.server.TagSubscription
import sample.server.ZeroQuery
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class RetryTest {
  @Test
  fun retryIsWorking() = runTest(skipDelays = false) {
    var sampleServer = SampleServer(tag = "tag1")
    ApolloClient.Builder()
        .serverUrl(sampleServer.graphqlUrl())
        .retryOnError { it.operation is Subscription }
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(sampleServer.subscriptionsUrl())
                .wsProtocol(SubscriptionWsProtocol { null })
                .build()
        )
        .build().use { apolloClient ->
          apolloClient.subscription(TagSubscription(intervalMillis = Int.MAX_VALUE))
              .toFlow()
              .test {
                val item1 = awaitItem()
                assertEquals("tag1", item1.data?.state?.tag)

                // Reuse the port to keep the url unchanged
                val port = sampleServer.subscriptionsUrl().extractPort()
                sampleServer.close()
                sampleServer = SampleServer(port, "tag2")

                val item2 = awaitItem()
                assertEquals("tag2", item2.data?.state?.tag)

                // The new sub MUST use a different id
                assertNotEquals(item1.data?.state?.subscriptionId, item2.data?.state?.subscriptionId)

                cancelAndIgnoreRemainingEvents()
              }
        }
    sampleServer.close()
  }

  @Test
  fun retryCanBeDisabled() = runTest(skipDelays = false) {
    val sampleServer = SampleServer(tag = "tag1")
    ApolloClient.Builder()
        .serverUrl(sampleServer.graphqlUrl())
        .retryOnError { it.operation is Subscription }
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(sampleServer.subscriptionsUrl())
                .wsProtocol(SubscriptionWsProtocol { null })
                .build()
        )
        .build().use { apolloClient ->
          apolloClient.subscription(TagSubscription(intervalMillis = Int.MAX_VALUE))
              .retryOnError(false)
              .toFlow()
              .test {
                val item1 = awaitItem()
                assertEquals("tag1", item1.data?.state?.tag)

                sampleServer.close()

                val item2 = awaitItem()
                assertIs<ApolloNetworkException>(item2.exception)
                awaitComplete()
              }
        }
  }

  @Test
  fun subscriptionsAreNotRetriedByDefault() = runTest(skipDelays = false) {
    val sampleServer = SampleServer(tag = "tag1")
    ApolloClient.Builder()
        .serverUrl(sampleServer.graphqlUrl())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(sampleServer.subscriptionsUrl())
                .wsProtocol(SubscriptionWsProtocol { null })
                .build()
        )
        .build().use { apolloClient ->
          apolloClient.subscription(TagSubscription(intervalMillis = Int.MAX_VALUE))
              .retryOnError(false)
              .toFlow()
              .test {
                val item1 = awaitItem()
                assertEquals("tag1", item1.data?.state?.tag)

                sampleServer.close()

                val item2 = awaitItem()
                assertIs<ApolloNetworkException>(item2.exception)
                awaitComplete()
              }
        }
  }

  @Test
  fun queriesAreNotRetriedWhenRetrySubscriptionsIsTrue() = mockServerTest(
      clientBuilder = {
        retryOnError { it.operation is Subscription }
      },
      skipDelays = false
  ) {
    mockServer.enqueue(MockResponse.Builder().statusCode(500).build())

    apolloClient.query(ZeroQuery())
        .toFlow()
        .test {
          assertIs<ApolloHttpException>(awaitItem().exception)
          awaitComplete()
        }
  }

  @Test
  fun websocketRetryDoNotPile() = runTest {
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
          delay(1000)
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
          delay(10_000)
          /**
           * Reopen the MockServer, the second item for each subscription should be emitted quickly after recovery.
           */
          mockServer = MockServer.Builder().port(port = port).build()
          prepareMockServer(mockServer, iterations)

          /**
           * I'm putting 5 here to be safe but in practice, this shouldn't take more than ~1s
           */
          withTimeout(5.seconds) {
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
      webSocket.enqueueMessage(ackMessage())

      repeat(repeat) {
        val operationId = webSocketRequest.awaitMessage().operationId()
        webSocket.enqueueMessage(FooSubscription.nextMessage(operationId, 42))
      }
    }
  }
}

private fun String.extractPort(): Int {
  return Regex("[a-z]*://[a-zA-Z0-9.]*:([0-9]*)").matchAt(this, 0)?.groupValues?.get(1)?.toInt() ?: error("No port found in $this")
}