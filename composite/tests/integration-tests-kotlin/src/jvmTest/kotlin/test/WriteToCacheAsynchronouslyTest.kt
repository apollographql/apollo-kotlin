package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.withStore
import com.apollographql.apollo3.cache.normalized.withWriteToCacheAsynchronously
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runBlocking
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import readResource
import java.util.concurrent.Executors
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WriteToCacheAsynchronouslyTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore
  private lateinit var dispatcher: CoroutineDispatcher

  @BeforeTest
  fun setUp() {
    dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient(
        serverUrl = mockServer.url(),
        requestedDispatcher = dispatcher
    ).withStore(store)
  }

  /**
   * Write to cache asynchronously, make sure records are not in cache when we receive the response
   */
  @Test
  fun writeToCacheAsynchronously() = runWithMainLoop(dispatcher) {
    val query = HeroAndFriendsNamesQuery(Episode.JEDI)

    mockServer.enqueue(readResource("HeroAndFriendsNameResponse.json"))
    apolloClient.query(
        ApolloRequest(query)
            .withWriteToCacheAsynchronously(true)
    )

    val record = store.accessCache { it.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE) }
    assertNull(record)
  }

  /**
   * Write to cache synchronously, make sure records are in cache when we receive the response
   */
  @Test
  fun writeToCacheSynchronously(): Unit = runBlocking(context = dispatcher) {
    val query = HeroAndFriendsNamesQuery(Episode.JEDI)

    mockServer.enqueue(readResource("HeroAndFriendsNameResponse.json"))
    apolloClient.query(
        ApolloRequest(query)
            .withWriteToCacheAsynchronously(false)
    )

    val record = store.accessCache { it.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE) }
    assertNotNull(record)
  }

  companion object {
    const val QUERY_ROOT_KEY = "QUERY_ROOT"
  }
}
