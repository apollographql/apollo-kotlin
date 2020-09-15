package com.apollographql.apollo

import com.apollographql.apollo.Utils.mockResponse
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.coroutines.toFlow
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.fetcher.ResponseFetcher
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SqliteNormalizedCacheTest {
  private lateinit var apolloClient: ApolloClient

  @get:Rule
  val server = MockWebServer()

  @Before
  @Throws(IOException::class)
  fun setUp() {
    val cache = SqlNormalizedCacheFactory("jdbc:sqlite:")
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .normalizedCache(cache)
        .build()
  }

  @Test
  fun `populating the cache triggers the watcher`() {
    runBlocking {
      val query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
      server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))

      val watchedResponses = mutableListOf<Response<EpisodeHeroNameQuery.Data>>()

      val deferred = async {
        apolloClient.query(query)
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
            .watcher()
            .toFlow()
            .collect {
              watchedResponses.add(it)
            }
      }

      val responses = apolloClient.query(query).toFlow().toList()

      Truth.assertThat(responses.size).isEqualTo(1)
      Truth.assertThat(responses[0].isFromCache).isEqualTo(false)

      withTimeout(1000) {
        while(watchedResponses.size < 2) {
          delay(100)
        }
        Truth.assertThat(watchedResponses.size).isEqualTo(2)
        Truth.assertThat(watchedResponses[0].data).isEqualTo(null)
        Truth.assertThat(watchedResponses[1].data?.hero()?.name()).isEqualTo("R2-D2")
        Truth.assertThat(watchedResponses[1].isFromCache).isEqualTo(true)
      }
      deferred.cancel()
    }
  }
}
