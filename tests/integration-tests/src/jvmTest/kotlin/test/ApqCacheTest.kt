package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import org.junit.Test
import kotlin.test.fail

class ApqCacheTest {
  /**
   * https://github.com/apollographql/apollo-kotlin/issues/4617
   */
  @Test
  fun apqAndCache() = runTest {
    val mockServer = MockServer()

    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    val query = HeroNameQuery()

    mockServer.enqueue(query, data)
    mockServer.enqueue(query, data)

    try {
     ApolloClient.Builder()
          .serverUrl(mockServer.url())
          // Note that mutations will always be sent as POST requests, regardless of these settings, as to avoid hitting caches.
          .autoPersistedQueries(
              // For the initial hashed query that does not send the actual Graphql document
              httpMethodForHashedQueries = HttpMethod.Get,
              // For the follow-up query that sends the full document if the initial hashed query was not found
              httpMethodForDocumentQueries = HttpMethod.Get
          )
          .normalizedCache(normalizedCacheFactory = MemoryCacheFactory(10 * 1024 * 1024))
          .build()
      fail("An exception was expected")
    } catch (e: Exception) {
      check(e.message!!.contains("Apollo: the normalized cache must be configured before the auto persisted queries"))
    }
  }
}
