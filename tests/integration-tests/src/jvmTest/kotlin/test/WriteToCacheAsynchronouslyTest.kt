package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.cache.normalized.writeToCacheAsynchronously
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.asCoroutineDispatcher
import testFixtureToUtf8
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * These tests are only on the JVM as on native all the cache operations are serialized so it's impossible to read the cache before it
 * has been written and confirm/infirm the test. Maybe we could do something with an AtomicReference or something like this
 */
class WriteToCacheAsynchronouslyTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore
  private var dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  private suspend fun setUp() {
    store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .requestedDispatcher(dispatcher)
        .store(store)
        .build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
    dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  }

  /**
   * Write to cache asynchronously, make sure records are not in cache when we receive the response
   */
  @Test
  fun writeToCacheAsynchronously() = runTest(dispatcher, { setUp() }, { tearDown() }) {
    val query = HeroAndFriendsNamesQuery(Episode.JEDI)

    mockServer.enqueue(testFixtureToUtf8("HeroAndFriendsNameResponse.json"))
    apolloClient.query(query)
        .writeToCacheAsynchronously(true)
        .execute()


    val record = store.accessCache { it.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE) }
    assertNull(record)
  }

  /**
   * Write to cache synchronously, make sure records are in cache when we receive the response
   */
  @Test
  fun writeToCacheSynchronously() = runTest(dispatcher, { setUp() }, { tearDown() }) {
    val query = HeroAndFriendsNamesQuery(Episode.JEDI)

    mockServer.enqueue(testFixtureToUtf8("HeroAndFriendsNameResponse.json"))
    apolloClient.query(query)
        .writeToCacheAsynchronously(false)
        .execute()

    val record = store.accessCache { it.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE) }
    assertNotNull(record)
  }

  companion object {
    const val QUERY_ROOT_KEY = "QUERY_ROOT"
  }
}
