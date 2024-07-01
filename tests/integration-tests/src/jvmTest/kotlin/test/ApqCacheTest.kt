package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.composeJsonResponse
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
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

    mockServer.enqueueString(query.composeJsonResponse(data))
    mockServer.enqueueString(query.composeJsonResponse(data))

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
