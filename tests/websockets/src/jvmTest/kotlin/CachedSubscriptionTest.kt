
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.cache.normalized.watch
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
import sample.server.TimeQuery
import sample.server.TimeSubscription
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
        apolloClient.query(TimeQuery())
            .watch()
            // Ignore cache miss
            .filter { it.data != null }
            .map { it.data!!.time }
            .collect {
              channel.send(it)
              println("watcher received: $it")
            }
      }

      assertEquals(0, channel.receive())

      println("starting subscription")
      apolloClient.subscription(TimeSubscription())
          .toFlow()
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
