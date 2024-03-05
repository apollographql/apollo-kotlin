package incubating

import app.cash.turbine.test
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.network.ws.incubating.SubscriptionWsProtocol
import com.apollographql.apollo3.network.ws.incubating.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.mockServerTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import sample.server.TagSubscription
import sample.server.TimeQuery
import kotlin.test.assertIs

class RetryTest {
  @Test
  fun retryIsWorking() = runTest(skipDelays = false) {
    // Hopefully it's really a free port, if not this test will fail
    val freePort = 8392
    var sampleServer = SampleServer(freePort, "tag1")
    ApolloClient.Builder()
        .serverUrl(sampleServer.graphqlUrl())
        .retryNetworkErrors(true)
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

                sampleServer.close()
                sampleServer = SampleServer(freePort, "tag2")

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
    // Hopefully it's really a free port, if not this test will fail
    val freePort = 8393
    val sampleServer = SampleServer(freePort, "tag1")
    ApolloClient.Builder()
        .serverUrl(sampleServer.graphqlUrl())
        .retryNetworkErrors(true)
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(sampleServer.subscriptionsUrl())
                .wsProtocol(SubscriptionWsProtocol { null })
                .build()
        )
        .build().use { apolloClient ->
          apolloClient.subscription(TagSubscription(intervalMillis = Int.MAX_VALUE))
              .retryNetworkErrors(false)
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
  fun queriesAreNotRetriedByDefault() = mockServerTest(skipDelays = false) {
    mockServer.enqueue(MockResponse.Builder().statusCode(500).build())

    apolloClient.query(TimeQuery())
        .toFlow()
        .test {
          assertIs<ApolloHttpException>(awaitItem().exception)
          awaitComplete()
        }
  }
}