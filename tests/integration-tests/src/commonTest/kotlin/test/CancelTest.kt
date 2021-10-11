package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.cache.normalized.queryCacheAndNetwork
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import readTestFixture
import kotlin.test.Test

class CancelTest {

  @Test
  fun cancelFlow() {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
    mockServer.enqueue(readTestFixture("resources/EpisodeHeroNameResponse.json"))

    runWithMainLoop {
      val job = launch {
        delay(100)
        apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
        error("The Flow should have been canceled before reaching that state")
      }
      job.cancel()
      job.join()
    }
  }


  @Test
  fun canCancelQueryCacheAndNetwork() {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).normalizedCache(MemoryCacheFactory()).build()

    mockServer.enqueue(readTestFixture("resources/EpisodeHeroNameResponse.json"), 500)

    runWithMainLoop {
      val sharedFlow = MutableSharedFlow<Unit>()

      val job = launch {
        apolloClient.queryCacheAndNetwork(EpisodeHeroNameQuery(Episode.EMPIRE)).toList()
      }
      delay(100)
      job.cancel()
    }
  }
}
