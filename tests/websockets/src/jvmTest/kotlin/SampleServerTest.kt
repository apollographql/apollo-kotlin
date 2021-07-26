import com.apollographql.apollo.sample.server.DefaultApplication
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import kotlinx.coroutines.delay
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
import kotlin.test.assertEquals

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
    val apolloClient = ApolloClient(
        networkTransport = WebSocketNetworkTransport(
            serverUrl = "http://localhost:8080/subscriptions"
        )
    )

    runBlocking {
      val list = apolloClient.subscribe(CountSubscription(5, 0))
          .map { it.data!!.count }
          .toList()
      assertEquals(0.until(5).toList(), list)
    }
  }

  @Test
  fun interleavedSubscriptions() {
    val apolloClient = ApolloClient(
        networkTransport = WebSocketNetworkTransport(
            serverUrl = "http://localhost:8080/subscriptions"
        )
    )

    runBlocking {
      val items = mutableListOf<Int>()
      launch {
        apolloClient.subscribe(CountSubscription(5, 1000))
            .collect {
              items.add(it.data!!.count * 2)
            }
      }
      delay(500)
      apolloClient.subscribe(CountSubscription(5, 1000))
          .collect {
            items.add(2 * it.data!!.count + 1)
          }
      assertEquals(0.until(10).toList(), items)
    }
  }

  @Test
  fun idleTimeout() {
    val transport = WebSocketNetworkTransport(
        serverUrl = "http://localhost:8080/subscriptions",
        idleTimeoutMillis = 1000
    )
    val apolloClient = ApolloClient(
        networkTransport = transport
    )

    runBlocking {
      apolloClient.subscribe(CountSubscription(50, 1000)).first()

      withTimeout(500) {
        transport.subscriptionCount.first { it == 0 }
      }

      delay(1500)
      val number = apolloClient.subscribe(CountSubscription(50, 0)).drop(3).first().data?.count
      assertEquals(3, number)
    }
  }

  @Test
  fun slowConsumer() {
    val apolloClient = ApolloClient(serverUrl = "http://localhost:8080/subscriptions")

    runBlocking {
      /**
       * Take 3 items, delaying the first items by 100ms in total.
       * During that time, the server should continue sending. Then resume reading as fast as we can
       * (which is still probably slower than the server) and make sure we didn't drop any items
       */
      val number = apolloClient.subscribe(CountSubscription(1000, 0))
          .map { it.data!!.count  }
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
    val transport = WebSocketNetworkTransport(
        serverUrl = "http://localhost:8080/subscriptions",
        idleTimeoutMillis = 1000
    )
    val apolloClient = ApolloClient(
        networkTransport = transport
    )
    runBlocking {
      /**
       * Collect all items the server sends us
       */
      apolloClient.subscribe(CountSubscription(50, 0)).toList()

      /**
       * Make sure we're unsubscribed
       */
      withTimeout(500) {
        transport.subscriptionCount.first { it == 0 }
      }
    }
  }
}