package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo.testing.internal.runTest
import defer.WithFragmentSpreadsSubscription
import defer.WithInlineFragmentsSubscription
import defer.fragment.CounterFields
import kotlinx.coroutines.flow.toList
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * This test is ignored on the CI because it requires a specific server to run.
 *
 * It can be manually tested by running the server from https://github.com/BoD/DeferDemo/tree/master/helix
 */
@Ignore
class DeferSubscriptionsTest {
  private lateinit var apolloClient: ApolloClient

  private fun setUp() {
    apolloClient = ApolloClient.Builder()
        .serverUrl("http://localhost:4000/graphql")
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl("ws://localhost:4000/graphql")
                .build()
        )
        .build()
  }

  private fun tearDown() {
    apolloClient.close()
  }

  @Test
  fun subscriptionWithInlineFragment() = runTest(before = { setUp() }, after = { tearDown() }) {
    val expectedDataList = listOf(
        // Emission 0, deferred payload 0
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 1, null)),
        // Emission 0, deferred payload 1
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 1, WithInlineFragmentsSubscription.OnCounter(2))),
        // Emission 1, deferred payload 0
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 2, null)),
        // Emission 1, deferred payload 1
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 2, WithInlineFragmentsSubscription.OnCounter(4))),
        // Emission 2, deferred payload 0
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 3, null)),
        // Emission 2, deferred payload 1
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 3, WithInlineFragmentsSubscription.OnCounter(6))),
    )

    val actualDataList = apolloClient.subscription(WithInlineFragmentsSubscription()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun subscriptionWithFragmentSpreads() = runTest(before = { setUp() }, after = { tearDown() }) {
    val expectedDataList = listOf(
        // Emission 0, deferred payload 0
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 1, null)),
        // Emission 0, deferred payload 1
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 1, CounterFields(2))),
        // Emission 1, deferred payload 0
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 2, null)),
        // Emission 1, deferred payload 1
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 2, CounterFields(4))),
        // Emission 2, deferred payload 0
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 3, null)),
        // Emission 2, deferred payload 1
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 3, CounterFields(6))),
    )

    val actualDataList = apolloClient.subscription(WithFragmentSpreadsSubscription()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

}
