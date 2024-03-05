package incubating

import app.cash.turbine.test
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.incubating.SubscriptionWsProtocol
import com.apollographql.apollo3.network.ws.incubating.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import sample.server.TagSubscription

class RetryTest {
  @Test
  fun retryIsWorking() = runTest {
    // Hopefully it's really a free port, if not this test will fail
    val freePort = 8392
    var sampleServer = SampleServer(freePort, "tag1")
    ApolloClient.Builder()
        .serverUrl(sampleServer.graphqlUrl())
        .retrySubscriptions(true)
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
  }
}