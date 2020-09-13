package com.apollographql.apollo

import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.mockResponse
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.coroutines.toFlow
import com.apollographql.apollo.coroutines.toJob
import com.apollographql.apollo.coroutines.toDeferred
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroNameWithIdQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class CoroutinesApolloTest {
  private lateinit var apolloClient: ApolloClient

  @get:Rule
  val server = MockWebServer()

  @Before
  fun setup() {
    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .dispatcher(immediateExecutor())
        .okHttpClient(okHttpClient)
        .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdFieldCacheKeyResolver())
        .build()
  }

  @Test
  fun callAwaitProducesValue() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

    runBlocking {
      val response: Response<EpisodeHeroNameQuery.Data> =
      apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).await()
      assertThat(response.data!!.hero()!!.name()).isEqualTo("R2-D2")
    }
  }

  @Test
  fun callDeferredProducesValue() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

    runBlocking {
        val response: Response<EpisodeHeroNameQuery.Data> =
        apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).toDeferred().await()
        assertThat(response.data!!.hero()!!.name()).isEqualTo("R2-D2")
    }
  }

  @Test
  fun prefetchCompletes() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

    runBlocking {
      apolloClient.prefetch(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).await()
    }
  }

  @Test
  fun await_prefetchIsCanceledWhenDisposed() {
    // Block for 5 seconds.
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID).throttleBody(1, 5000, TimeUnit.MILLISECONDS))

    runBlocking {
      val prefetchCall = apolloClient.prefetch(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
      val job = async {
        prefetchCall.await()
      }
      job.cancel()
    }
  }

  @Test
  fun toJob_prefetchIsCanceledWhenDisposed() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

    runBlocking {
      val job = apolloClient.prefetch(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
          .toJob()
      job.cancel()
    }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun flowCanBeRead() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

    val flow = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).toFlow()

    runBlocking {
      val result = mutableListOf<Response<EpisodeHeroNameQuery.Data>>()
      flow.toList(result)
      assertThat(result.size).isEqualTo(1)
      assertThat(result[0].data?.hero()?.name()).isEqualTo("R2-D2")
    }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun flowError() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("nonsense"))

    val flow = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).toFlow()

    runBlocking {
      val result = mutableListOf<Response<EpisodeHeroNameQuery.Data>>()
      try {
        flow.toList(result)
      } catch (e: ApolloException) {
        return@runBlocking
      }

      throw Exception("exception has not been thrown")
    }
  }

  @Test
  @ExperimentalCoroutinesApi
  fun callFlowRetry() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("nonsense"))
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

    val response: Response<EpisodeHeroNameQuery.Data> = runBlocking {
      apolloClient
          .query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
          .toFlow()
          .retry(retries = 1)
          .single()
    }

    assertThat(response.data!!.hero()!!.name()).isEqualTo("R2-D2")
  }

  @Test
  @ExperimentalCoroutinesApi
  fun watcherFlowRetry() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("nonsense"))
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

    val response: Response<EpisodeHeroNameQuery.Data> = runBlocking {
      apolloClient
          .query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
          .watcher()
          .toFlow()
          .retry(retries = 1)
          .first()
    }

    assertThat(response.data!!.hero()!!.name()).isEqualTo("R2-D2")
  }

  @Test
  @ExperimentalCoroutinesApi
  fun watcherFlowCancellationCancelsWatcher(): Unit = runBlocking {
    server.enqueue(mockResponse("HeroNameWithIdResponse.json"))
    apolloClient
        .query(HeroNameWithIdQuery())
        .watcher()
        .toFlow()
        .first() // Cancels the flow after first response

    apolloClient.clearNormalizedCache()
    apolloClient.clearHttpCache()

    server.enqueue(mockResponse("HeroNameResponse.json"))
    apolloClient
        .query(HeroNameQuery())
        .watcher()
        .toFlow()
        .first()

    assertThat(server.requestCount).isEqualTo(2)
  }.let { }

  companion object {

    private const val FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json"
  }
}
