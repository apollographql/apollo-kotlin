package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.queryCacheAndNetwork
import com.apollographql.apollo3.cache.normalized.withNormalizedCache
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import readTestFixture
import kotlin.test.BeforeTest
import kotlin.test.Test

class CancelTest {

  @Test
  fun cancelFlow() {
    val mockServer = MockServer()
    val apolloClient = ApolloClient(mockServer.url())
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
    val apolloClient = ApolloClient(mockServer.url()).withNormalizedCache(MemoryCacheFactory())

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
