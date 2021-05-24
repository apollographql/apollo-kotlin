package com.apollographql.apollo.internal.batch

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.mockResponse
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
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

class BatchHttpCallTest {

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
        .batchingConfiguration(BatchConfig(batchingEnabled = true, batchIntervalMs = 50, maxBatchSize = 10))
        .build()
    apolloClient.startBatchPoller()
  }

  @After
  fun tearDown() {
    apolloClient.stopBatchPoller()
    server.shutdown()
  }

  @Test
  fun testMultipleQueryBatchingSuccess() {
    val planetsQuery = AllPlanetsQuery()
    val episodeQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    val planetsCallback = PlanetsCallback()
    val episodeCallback = EpisodeCallback()

    server.enqueue(mockResponse("BatchQueryResponse.json"))
    apolloClient.batchQuery(planetsQuery).enqueue(planetsCallback)
    apolloClient.batchQuery(episodeQuery).enqueue(episodeCallback)
    assertThat(planetsCallback.responseList).isEmpty()
    assertThat(episodeCallback.responseList).isEmpty()
    Thread.sleep(100) // wait for batch call to trigger
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
    apolloClient.batchQuery(planetsQuery).enqueue(planetsCallback)
    apolloClient.batchQuery(episodeQuery).enqueue(episodeCallback)
    Thread.sleep(100) // wait for batch call to trigger
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