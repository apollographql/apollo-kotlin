package com.apollographql.apollo.internal.batch

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.mockResponse
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.fetcher.ResponseFetcher
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.request.RequestHeaders
import com.google.common.truth.Truth.assertThat
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

class BatchHttpResponseTest {

  lateinit var apolloClient: ApolloClient
  lateinit var server: MockWebServer

  @Before
  fun setUp() {
    server = MockWebServer()
    val okHttpClient = OkHttpClient.Builder()
        .writeTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .dispatcher(immediateExecutor())
        .batchingConfiguration(BatchConfig(batchingEnabled = true, batchIntervalMs = 2000, maxBatchSize = 2))
        .build()
    apolloClient.startBatchPoller()
  }

  @After
  fun tearDown() {
    apolloClient.stopBatchPoller()
    server.shutdown()
  }

  @Test
  fun testBatchingDisabled() {
    val planetsQuery = AllPlanetsQuery()
    val episodeQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    val planetsCallback = PlanetsCallback()
    val episodeCallback = EpisodeCallback()

    server.enqueue(mockResponse("AllPlanetsNullableField.json"))
    apolloClient.query(planetsQuery).toBuilder().canBeBatched(false).build().enqueue(planetsCallback)
    assertThat(planetsCallback.exceptions).isEmpty()
    assertThat(planetsCallback.responseList[0].data?.allPlanets()?.planets()?.size).isEqualTo(60)

    server.enqueue(mockResponse("EpisodeHeroNameResponse.json"))
    apolloClient.query(episodeQuery).toBuilder().canBeBatched(false).build().enqueue(episodeCallback)
    assertThat(episodeCallback.exceptions).isEmpty()
    assertThat(episodeCallback.responseList[0].data?.hero()?.name()).isEqualTo("R2-D2")
  }

  @Test
  fun testMultipleQueryBatchingSuccess() {
    val planetsQuery = AllPlanetsQuery()
    val episodeQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    val planetsCallback = PlanetsCallback()
    val episodeCallback = EpisodeCallback()

    server.enqueue(mockResponse("BatchQueryResponse.json"))
    apolloClient.query(planetsQuery).toBuilder().canBeBatched(true).build().enqueue(planetsCallback)
    apolloClient.query(episodeQuery).toBuilder().canBeBatched(true).build().enqueue(episodeCallback)
    assertThat(planetsCallback.exceptions).isEmpty()
    assertThat(episodeCallback.exceptions).isEmpty()
    assertThat(planetsCallback.responseList[0].data?.allPlanets()?.planets()?.size).isEqualTo(60)
    assertThat(episodeCallback.responseList[0].data?.hero()?.name()).isEqualTo("R2-D2")
  }

  @Test
  fun testMultipleClonedQueryBatchingSuccess() {
    val planetsQuery = AllPlanetsQuery()
    val episodeQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    val planetsCallback = PlanetsCallback()
    val episodeCallback = EpisodeCallback()
    val ogPlanetQuery = apolloClient.query(planetsQuery).toBuilder().canBeBatched(true).build()
    val ogEpisodeQuery = apolloClient.query(episodeQuery).toBuilder().canBeBatched(true).build()

    server.enqueue(mockResponse("BatchQueryResponse.json"))
    ogPlanetQuery.toBuilder().responseFetcher(ApolloResponseFetchers.NETWORK_FIRST).build().enqueue(planetsCallback)
    ogEpisodeQuery.toBuilder().requestHeaders(RequestHeaders.NONE).build().enqueue(episodeCallback)
    assertThat(planetsCallback.exceptions).isEmpty()
    assertThat(episodeCallback.exceptions).isEmpty()
    assertThat(planetsCallback.responseList[0].data?.allPlanets()?.planets()?.size).isEqualTo(60)
    assertThat(episodeCallback.responseList[0].data?.hero()?.name()).isEqualTo("R2-D2")
  }

  @Test
  fun testMultipleQueryBatchingError() {
    val planetsQuery = AllPlanetsQuery()
    val episodeQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    val planetsCallback = PlanetsCallback()
    val episodeCallback = EpisodeCallback()

    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR).setBody("Server Error"))
    apolloClient.query(planetsQuery).toBuilder().canBeBatched(true).build().enqueue(planetsCallback)
    apolloClient.query(episodeQuery).toBuilder().canBeBatched(true).build().enqueue(episodeCallback)
    assertThat(planetsCallback.exceptions.size).isEqualTo(1)
    assertThat(episodeCallback.exceptions.size).isEqualTo(1)
  }

  class PlanetsCallback : ApolloCall.Callback<AllPlanetsQuery.Data>() {
    val responseList: MutableList<Response<AllPlanetsQuery.Data>> = ArrayList()
    val exceptions: MutableList<Exception> = ArrayList()

    @Volatile
    var completed = false
    override fun onResponse(response: Response<AllPlanetsQuery.Data>) {
      check(!completed) { "onCompleted already called Do not reuse callback." }
      responseList.add(response)
    }

    override fun onFailure(e: ApolloException) {
      check(!completed) { "onCompleted already called Do not reuse callback." }
      exceptions.add(e)
    }

    override fun onStatusEvent(event: ApolloCall.StatusEvent) {
      if (event == ApolloCall.StatusEvent.COMPLETED) {
        check(!completed) { "onCompleted already called Do not reuse callback." }
        completed = true
      }
    }
  }

  class EpisodeCallback : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
    val responseList: MutableList<Response<EpisodeHeroNameQuery.Data>> = ArrayList()
    val exceptions: MutableList<Exception> = ArrayList()

    @Volatile
    var completed = false
    override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
      check(!completed) { "onCompleted already called Do not reuse callback." }
      responseList.add(response)
    }

    override fun onFailure(e: ApolloException) {
      check(!completed) { "onCompleted already called Do not reuse callback." }
      exceptions.add(e)
    }

    override fun onStatusEvent(event: ApolloCall.StatusEvent) {
      if (event == ApolloCall.StatusEvent.COMPLETED) {
        check(!completed) { "onCompleted already called Do not reuse callback." }
        completed = true
      }
    }
  }
}