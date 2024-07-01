
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.cache.normalized.watch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import sample.server.ValueSharedWithSubscriptionsQuery
import sample.server.ValueSharedWithSubscriptionsSubscription
import kotlin.test.assertEquals

class CachedSubscriptionTest {
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
  fun subscriptionsCanUpdateTheCache() {
    val store = ApolloStore(
        MemoryCacheFactory(Int.MAX_VALUE),
    )

    val apolloClient = ApolloClient.Builder()
        .httpServerUrl(sampleServer.graphqlUrl())
        .webSocketServerUrl(sampleServer.subscriptionsUrl())
        .store(store)
        .build()

    runBlocking {
      val channel = Channel<Int>()
      val job = launch {
        apolloClient.query(ValueSharedWithSubscriptionsQuery())
            .watch()
            // Ignore cache miss
            .filter { it.data != null }
            .map { it.data!!.valueSharedWithSubscriptions }
            .collect {
              channel.send(it)
            }
      }

      assertEquals(0, channel.receive())

      apolloClient.subscription(ValueSharedWithSubscriptionsSubscription())
          .toFlow()
          .take(3)
          .map { it.data!!.valueSharedWithSubscriptions }
          .collect {
          }

      withTimeout(600) {
        assertEquals(listOf(1, 2), channel.consumeAsFlow().take(2).toList())
      }
      job.cancel()
    }
  }
}
