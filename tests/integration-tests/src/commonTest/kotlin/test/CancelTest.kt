package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import testFixtureToUtf8
import kotlin.test.Test

class CancelTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.close()
  }

  @Test
  fun cancelFlow() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8("EpisodeHeroNameResponse.json"))
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()

    val job = launch {
      delay(100)
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE)).execute()
      error("The Flow should have been canceled before reaching that state")
    }
    job.cancel()
    job.join()
  }

  @Test
  fun canCancelQueryCacheAndNetwork() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8("EpisodeHeroNameResponse.json"), 500)
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).normalizedCache(MemoryCacheFactory()).build()

    val job = launch {
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE)).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().toList()
    }
    delay(100)
    job.cancel()
  }
}
