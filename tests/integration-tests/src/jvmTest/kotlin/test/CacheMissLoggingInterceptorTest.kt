package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.CacheMissLoggingInterceptor
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.logCacheMisses
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.integration.normalizer.HeroAppearsInQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * We're only doing this on the JVM because it saves time and the CacheMissLoggingInterceptor
 * touches mutable data from different threads
 */
class CacheMissLoggingInterceptorTest {

  @Test
  fun cacheMissLogging() = runTest {
    val recordedLogs = mutableListOf<String>()
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .logCacheMisses {
          synchronized(recordedLogs) {
            recordedLogs.add(it)
          }
        }
        .normalizedCache(MemoryCacheFactory())
        .build()

    mockServer.enqueue("""
      {
        "data": {
          "hero": {
            "name": "Luke"
          }
        }
      }
    """.trimIndent())
    apolloClient.query(HeroNameQuery()).execute()
    try {
      apolloClient.query(HeroAppearsInQuery()).fetchPolicy(FetchPolicy.CacheOnly).execute()
      error("An exception was expected")
    } catch (_: ApolloException) {
    }

    assertEquals(
        listOf(
            "Object 'QUERY_ROOT' has no field named 'hero'",
            "Object 'hero' has no field named 'appearsIn'"
        ),
        recordedLogs
    )
    mockServer.stop()
    apolloClient.dispose()
  }

  @Test
  fun logCacheMissesMustBeCalledFirst() {
    try {
      ApolloClient.Builder()
          .normalizedCache(MemoryCacheFactory())
          .logCacheMisses()
          .build()
      error("We expected an exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("logCacheMisses() must be called before setting up your normalized cache") == true)
    }
  }
}