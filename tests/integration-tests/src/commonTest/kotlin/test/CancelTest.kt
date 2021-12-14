package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.executeCacheAndNetwork
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import testFixtureToUtf8
import kotlin.test.Test

@OptIn(ApolloExperimental::class)
class CancelTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun cancelFlow() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(testFixtureToUtf8("EpisodeHeroNameResponse.json"))
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
    mockServer.enqueue(testFixtureToUtf8("EpisodeHeroNameResponse.json"), 500)
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).normalizedCache(MemoryCacheFactory()).build()

    val job = launch {
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE)).executeCacheAndNetwork().toList()
    }
    delay(100)
    job.cancel()
  }
}
