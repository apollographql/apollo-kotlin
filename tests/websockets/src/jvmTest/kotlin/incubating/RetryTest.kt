package incubating

import app.cash.turbine.test
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.network.websocket.SubscriptionWsProtocol
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.mockServerTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import sample.server.TagSubscription
import sample.server.ZeroQuery
import kotlin.test.assertIs

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
}

private fun String.extractPort(): Int {
  return Regex("[a-z]*://[a-zA-Z0-9.]*:([0-9]*)").matchAt(this, 0)?.groupValues?.get(1)?.toInt() ?: error("No port found in $this")
}