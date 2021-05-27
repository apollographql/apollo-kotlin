import com.apollographql.apollo.sample.server.DefaultApplication
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.MemoryCache
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.interceptor.cache.watch
import com.apollographql.apollo3.interceptor.cache.withStore
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.network.ws.ApolloWebSocketNetworkTransport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import sample.server.CountQuery
import sample.server.CountSubscription
import kotlin.test.assertEquals

class CachedSubscriptionTest {
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
  fun subscriptionsCanUpdateTheCache() {
    val store = ApolloStore(MemoryCacheFactory(Int.MAX_VALUE))

    val apolloClient = ApolloClient(
        networkTransport = ApolloHttpNetworkTransport(serverUrl = "http://localhost:8080/graphql"),
        subscriptionNetworkTransport = ApolloWebSocketNetworkTransport(
            serverUrl = "http://localhost:8080/subscriptions"
        )
    ).withStore(store)

    runBlocking {
      val channel = Channel<Int>()
      launch {
        apolloClient.watch(CountQuery())
            .collect {
              channel.send(it.data!!.count)
            }
      }

      assertEquals(0, channel.receive())

      apolloClient.subscribe(CountSubscription(5, 0))
          .map { it.data!!.count }
          .toList()

      withTimeout(600) {
        assertEquals(listOf(1, 2, 3, 4), channel.toList())
      }
    }
  }
}