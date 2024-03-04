package incubating

import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.SubscriptionOperationException
import com.apollographql.apollo3.network.ws.incubating.SubscriptionWsProtocol
import com.apollographql.apollo3.network.ws.incubating.WebSocketNetworkTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import sample.server.CountSubscription
import sample.server.OperationErrorSubscription
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SampleServerTest {
  private class Scope(val apolloClient: ApolloClient, val sampleServer: SampleServer, val coroutineScope: CoroutineScope)

  private fun test(customizeTransport: WebSocketNetworkTransport.Builder.() -> Unit = {}, block: suspend Scope.() -> Unit) {
    runBlocking {
      SampleServer().use { sampleServer ->
        ApolloClient.Builder()
            .serverUrl(sampleServer.graphqlUrl())
            .subscriptionNetworkTransport(
                WebSocketNetworkTransport.Builder()
                    .serverUrl(sampleServer.subscriptionsUrl())
                    .wsProtocol(SubscriptionWsProtocol { null })
                    .apply(customizeTransport)
                    .build()
            )
            .build().use { apolloClient ->
              Scope(apolloClient, sampleServer, this@runBlocking).block()
            }
      }
    }
  }


  @Test
  fun simple() = test {
    val list = apolloClient.subscription(CountSubscription(5, 0))
        .toFlow()
        .map {
          it.data?.count
        }
        .toList()
    assertEquals(0.until(5).toList(), list)
  }

  @Test
  fun interleavedSubscriptions() = test {
    val items = mutableListOf<Int>()
    coroutineScope.launch {
      apolloClient.subscription(CountSubscription(5, 1000))
          .toFlow()
          .collect {
            items.add(it.data!!.count * 2)
          }
    }
    delay(500)
    apolloClient.subscription(CountSubscription(5, 1000))
        .toFlow()
        .collect {
          items.add(2 * it.data!!.count + 1)
        }
    assertEquals(0.until(10).toList(), items)

  }

  @Test
  fun slowConsumer() = test {
    /**
     * Simulate a low read on the first 5 items.
     * During that time, the server should continue sending.
     * Then resume reading as fast as possible and make sure we didn't drop any items.
     */
    val items = apolloClient.subscription(CountSubscription(1000, 0))
        .toFlow()
        .map { it.data!!.count }
        .onEach {
          if (it < 5) {
            delay(100)
          }
        }
        .toList()

    assertEquals(0.until(1000).toList(), items)
  }

  @Test
  fun operationError() = test {
    val response = apolloClient.subscription(OperationErrorSubscription())
        .toFlow()
        .single()
    assertIs<SubscriptionOperationException>(response.exception)
    val error = response.exception.cast<SubscriptionOperationException>().payload
        .cast<Map<String, String>>()
        .get("message")
    assertEquals("Woops", error)
  }
}

@Suppress("UNCHECKED_CAST")
private fun <T> Any?.cast() = this as T

