package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.MemoryCache
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.mpp.Platform
import com.apollographql.apollo3.mpp.currentThreadId
import com.apollographql.apollo3.mpp.platform
import com.apollographql.apollo3.testing.QueueTestNetworkTransport
import com.apollographql.apollo3.testing.enqueueTestResponse
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import testFixtureToUtf8
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class ThreadTests {
  class MyNormalizedCache(val mainThreadId: String) : NormalizedCache() {
    val delegate = MemoryCache()

    override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
      check(currentThreadId() != mainThreadId) {
        "Cache access on main thread"
      }
      return delegate.merge(record, cacheHeaders)
    }

    override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
      check(currentThreadId() != mainThreadId) {
        "Cache access on main thread"
      }
      return delegate.merge(records, cacheHeaders)
    }

    override fun clearAll() {
      check(currentThreadId() != mainThreadId) {
        "Cache access on main thread"
      }
      return delegate.clearAll()
    }

    override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
      check(currentThreadId() != mainThreadId) {
        "Cache access on main thread"
      }
      return delegate.remove(cacheKey, cascade)
    }

    override fun remove(pattern: String): Int {
      check(currentThreadId() != mainThreadId) {
        "Cache access on main thread"
      }
      return delegate.remove(pattern)
    }

    override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
      check(currentThreadId() != mainThreadId) {
        "Cache access on main thread"
      }
      return delegate.loadRecord(key, cacheHeaders)
    }

    override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
      check(currentThreadId() != mainThreadId) {
        "Cache access on main thread"
      }
      return delegate.loadRecords(keys, cacheHeaders)
    }

    override fun dump(): Map<KClass<*>, Map<String, Record>> {
      check(currentThreadId() != mainThreadId) {
        "Cache access on main thread"
      }
      return delegate.dump()
    }
  }

  class MyMemoryCacheFactory(val mainThreadId: String) : NormalizedCacheFactory() {
    override fun create(): NormalizedCache {
      return MyNormalizedCache(mainThreadId)
    }

  }

  @Test
  fun cacheIsNotReadFromTheMainThread() = runTest {
    if (platform() == Platform.Js) {
      return@runTest
    }

    val apolloClient = ApolloClient.Builder()
        .normalizedCache(MyMemoryCacheFactory(currentThreadId()))
        .networkTransport(QueueTestNetworkTransport())
        .build()

    val data = HeroNameQuery.Data(HeroNameQuery.Hero("Luke"))
    val query = HeroNameQuery()
    apolloClient.enqueueTestResponse(query, data)

    apolloClient.query(query).execute()
    apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
    apolloClient.close()
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun usingKotlinxRunTest() = kotlinx.coroutines.test.runTest {
    val mockServer = MockServer()
    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))

    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
    val response = apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE)).execute()
    mockServer.stop()
    assertEquals(response.data?.hero?.name, "R2-D2")
  }
}
