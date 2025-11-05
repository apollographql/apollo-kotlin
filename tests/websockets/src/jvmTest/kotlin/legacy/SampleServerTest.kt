@file:Suppress("DEPRECATION")

package legacy

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.SubscriptionOperationException
import com.apollographql.apollo.interceptor.RetryOnErrorInterceptor
import com.apollographql.apollo.network.websocket.SubscriptionWsProtocol
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo.testing.internal.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import sample.server.CountSubscription
import sample.server.GraphqlAccessErrorSubscription
import sample.server.OperationErrorSubscription
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds

class SampleServerTest {
  companion object {
    private lateinit var sampleServer: SampleServer

    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      sampleServer = SampleServer()
    }

    @AfterClass
    @JvmStatic
    fun afterClass() {
      sampleServer.close()
    }
  }

  @Test
  fun simple() = runTest {
    apolloClient().use { apolloClient ->
      val list = apolloClient.subscription(CountSubscription(5, 0))
          .toFlow()
          .map {
            it.data?.count
          }
          .toList()
      assertEquals(0.until(5).toList(), list)
    }
  }

  private fun apolloClientBuilder(block: WebSocketNetworkTransport.Builder.() -> Unit = {}) = ApolloClient.Builder()
      .subscriptionNetworkTransport(
          WebSocketNetworkTransport.Builder()
              .wsProtocol(SubscriptionWsProtocol())
              .serverUrl(sampleServer.subscriptionsUrl())
              .apply(block)
              .build()
      )

  private fun apolloClient(block: WebSocketNetworkTransport.Builder.() -> Unit = {}): ApolloClient {
    return apolloClientBuilder(block).build()
  }

  @Test
  fun interleavedSubscriptions() = runTest {
    apolloClient().use { apolloClient ->
      val items = mutableListOf<Int>()
      launch {
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
  }

  @Test
  fun idleTimeout() = runTest {
    apolloClient {
      idleTimeout(1000.milliseconds)
    }.use { apolloClient ->
      apolloClient.subscription(CountSubscription(50, 1000)).toFlow().first()

      delay(1500)
      val number = apolloClient.subscription(CountSubscription(50, 0)).toFlow().drop(3).first().data?.count
      assertEquals(3, number)
    }
  }

  @Test
  fun slowConsumer() = runTest {
    apolloClient().use { apolloClient ->
      /**
       * Take 3 items, delaying the first items by 100ms in total.
       * During that time, the server should continue sending. Then resume reading as fast as we can
       * (which is still probably slower than the server) and make sure we didn't drop any items
       */
      val number = apolloClient.subscription(CountSubscription(1000, 0))
          .toFlow()
          .map { it.data!!.count }
          .onEach {
            if (it < 3) {
              delay(100)
            }
          }
          .drop(500)
          .first()

      assertEquals(500, number)
    }
  }

  @Test
  fun serverTermination() = runTest {
    apolloClient().use { apolloClient ->
      /**
       * Collect all items the server sends us
       */
      apolloClient.subscription(CountSubscription(50, 0)).toFlow().toList()
    }
  }

  @Test
  fun operationError() = runTest {
    apolloClient().use { apolloClient ->
      val response = apolloClient.subscription(OperationErrorSubscription())
          .toFlow()
          .single()
      assertIs<SubscriptionOperationException>(response.exception)
      val error = response.exception.cast<SubscriptionOperationException>().payload
          .cast<Map<String, String>>()
          .get("message")
      assertEquals("Error collecting the source event stream: Woops", error)
    }
  }

  private inline fun <reified T> Any?.cast() = this as T


  @Test
  fun canResumeAfterGraphQLError() = runTest {
    apolloClientBuilder()
        .retryOnErrorInterceptor(RetryOnErrorInterceptor {
          val error = it.response.errors.orEmpty().firstOrNull()
          if (error != null) {
            return@RetryOnErrorInterceptor true
          } else {
            return@RetryOnErrorInterceptor false
          }
        })
        .build().use { apolloClient ->
          val list = apolloClient.subscription(GraphqlAccessErrorSubscription(1))
              .toFlow()
              .map {
                it.data?.graphqlAccessError?.foo
              }
              .take(2)
              .toList()
          assertEquals(listOf(42, 42), list)
        }
  }
}