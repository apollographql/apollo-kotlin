import com.apollographql.apollo.sample.server.DefaultApplication
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.interceptor.cache.watch
import com.apollographql.apollo3.interceptor.cache.withStore
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import sample.server.TimeQuery
import sample.server.TimeSubscription
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
    val store = ApolloStore(
        MemoryCacheFactory(Int.MAX_VALUE),
        object : CacheResolver() {
          override fun cacheKeyForObject(field: CompiledField, variables: Executable.Variables, map: Map<String, Any?>): CacheKey {
            return CacheKey(field.responseName)
          }
        }
    )

    val apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(serverUrl = "http://localhost:8080/graphql"),
        subscriptionNetworkTransport = WebSocketNetworkTransport(
            serverUrl = "http://localhost:8080/subscriptions"
        )
    ).withStore(store)

    runBlocking {
      val channel = Channel<Int>()
      val job = launch {
        apolloClient.watch(TimeQuery())
            .map { it.data!!.time }
            .collect {
              channel.send(it)
              println("watcher received: $it")
            }
      }

      assertEquals(0, channel.receive())

      println("starting subscription")
      apolloClient.subscribe(TimeSubscription())
          .take(3)
          .map { it.data!!.time }
          .collect {
            println("subscription received: $it")
          }
      
      withTimeout(600) {
        assertEquals(listOf(1, 2), channel.consumeAsFlow().take(2).toList())
      }
      job.cancel()
    }
  }
}