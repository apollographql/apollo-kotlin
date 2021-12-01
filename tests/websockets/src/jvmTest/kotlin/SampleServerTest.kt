import com.apollographql.apollo.sample.server.DefaultApplication
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import sample.server.CountSubscription
import sample.server.OperationErrorSubscription
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SampleServerTest {
  companion object {
    private lateinit var context: ConfigurableApplicationContext

    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      context = runApplication<DefaultApplication>()
    }

    @AfterClass
    @JvmStatic
    fun afterClass() {
      context.close()
    }
  }

  @Test
  fun simple() {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .build()

    runBlocking {
      val list = apolloClient.subscription(CountSubscription(5, 0))
          .toFlow()
          .map { it.data!!.count }
          .toList()
      assertEquals(0.until(5).toList(), list)
    }
  }

  @Test
  fun interleavedSubscriptions() {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .build()

    runBlocking {
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
  fun idleTimeout() {
    val transport = WebSocketNetworkTransport.Builder().serverUrl(
        serverUrl = "http://localhost:8080/subscriptions",
    ).idleTimeoutMillis(
        idleTimeoutMillis = 1000
    ).build()

    val apolloClient = ApolloClient.Builder()
        .networkTransport(transport)
        .build()

    runBlocking {
      apolloClient.subscription(CountSubscription(50, 1000)).toFlow().first()

      withTimeout(500) {
        transport.subscriptionCount.first { it == 0 }
      }

      delay(1500)
      val number = apolloClient.subscription(CountSubscription(50, 0)).toFlow().drop(3).first().data?.count
      assertEquals(3, number)
    }
  }

  @Test
  fun slowConsumer() {
    val apolloClient = ApolloClient.Builder().serverUrl(serverUrl = "http://localhost:8080/subscriptions").build()

    runBlocking {
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
  fun serverTermination() {
    val transport = WebSocketNetworkTransport.Builder().serverUrl(
        serverUrl = "http://localhost:8080/subscriptions",
    ).idleTimeoutMillis(
        idleTimeoutMillis = 1000
    ).build()

    val apolloClient = ApolloClient.Builder()
        .networkTransport(transport)
        .build()
    runBlocking {
      /**
       * Collect all items the server sends us
       */
      apolloClient.subscription(CountSubscription(50, 0)).toFlow().toList()

      /**
       * Make sure we're unsubscribed
       */
      withTimeout(500) {
        transport.subscriptionCount.first { it == 0 }
      }
    }
  }

  @Test
  fun operationError() {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .build()

    runBlocking {
      var caught: Throwable? = null
      apolloClient.subscription(OperationErrorSubscription())
          .toFlow()
          .catch {
            caught = it
          }
          .collect()
      assertIs<ApolloNetworkException>(caught)
      assertTrue(caught!!.message!!.contains("Woops"))
    }
  }
}
