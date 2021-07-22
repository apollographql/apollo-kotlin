package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ClientScope
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.IdCacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.withStore
import com.apollographql.apollo3.cache.normalized.withWriteToCacheAsynchronously
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import readResource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WriteToCacheAsynchronouslyTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore
  private lateinit var dispatcher: CoroutineDispatcher

  private suspend fun setUp() {
    dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient(
        serverUrl = mockServer.url(),
        requestedDispatcher = dispatcher
    ).withStore(store)
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  /**
   * Write to cache asynchronously, make sure records are not in cache when we receive the response
   */
  @Test
  fun writeToCacheAsynchronously() = runTest(dispatcher, { setUp() }, { tearDown() }) {
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
  fun writeToCacheSynchronously() = runTest(dispatcher, { setUp() }, { tearDown() }) {
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
