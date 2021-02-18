package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.checkTestFixture
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.readFileToString
import com.apollographql.apollo3.api.CustomScalarAdapter
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.JsonElement
import com.apollographql.apollo3.api.JsonString
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Response
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.coroutines.await
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.http.OkHttpExecutionContext
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.httpcache.type.CustomScalars
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.google.common.base.Charsets
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.Locale

class IntegrationTest {
  private lateinit var apolloClient: ApolloClient
  private var dateCustomScalarAdapter = object : CustomScalarAdapter<Date> {
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun decode(jsonElement: JsonElement) = DATE_FORMAT.parse(jsonElement.toRawValue().toString())
    override fun encode(value: Date) = JsonString(DATE_FORMAT.format(value))
  }

  val server = MockWebServer()

  @Before
  fun setUp() {
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(OkHttpClient.Builder().dispatcher(Dispatcher(immediateExecutorService())).build())
        .addCustomScalarAdapter(CustomScalars.Date, dateCustomScalarAdapter)
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver())
        .defaultResponseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
        .dispatcher(immediateExecutor())
        .build()
  }

  @Test
  fun `request POST body contains operation, query and variables`() {
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"))
    assertResponse(apolloClient.query(AllPlanetsQuery())) {}
    val body = server.takeRequest().body.readString(Charsets.UTF_8)
    checkTestFixture(body, "IntegrationTest/allPlanets.json")
  }

  @Test
  @Throws(Exception::class)
  fun statusEvents() {
    server.enqueue(mockResponse("HeroNameResponse.json"))
    var statusEvents = enqueueCall(apolloClient.query(HeroNameQuery()))
    assertThat(statusEvents).isEqualTo(listOf(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent.FETCH_NETWORK, ApolloCall.StatusEvent.COMPLETED))
    statusEvents = enqueueCall(
        apolloClient.query(HeroNameQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY))
    assertThat(statusEvents).isEqualTo(listOf(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent.FETCH_CACHE, ApolloCall.StatusEvent.COMPLETED))
    server.enqueue(mockResponse("HeroNameResponse.json"))
    statusEvents = enqueueCall(
        apolloClient.query(HeroNameQuery()).responseFetcher(ApolloResponseFetchers.CACHE_AND_NETWORK))
    assertThat(statusEvents).isEqualTo(listOf(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent.FETCH_CACHE, ApolloCall.StatusEvent.FETCH_NETWORK, ApolloCall.StatusEvent.COMPLETED))
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseContainsHttpExecutionContext() {
    val httpResponse = mockResponse("HttpCacheTestAllPlanets.json")
        .setHeader("Header1", "Header1#value")
        .setHeader("Header2", "Header2#value")
    server.enqueue(httpResponse)
    assertResponse(
        apolloClient.query(AllPlanetsQuery())
    ) { response ->
      assertThat(response.executionContext[OkHttpExecutionContext.KEY]).isNotNull()
      assertThat(response.executionContext[OkHttpExecutionContext.KEY]!!.response).isNotNull()
      assertThat(response.executionContext[OkHttpExecutionContext.KEY]!!.response.headers().toString())
          .isEqualTo(
              """
              Transfer-encoding: chunked
              Header1: Header1#value
              Header2: Header2#value
              
              """.trimIndent()
          )
      assertThat(response.executionContext[OkHttpExecutionContext.KEY]!!.response.body()).isNull()
    }
  }

  @Test
  @Throws(Exception::class)
  fun writeOperationRawRequest() {
    val query = EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))

    assertThat(query.name()).isEqualTo("EpisodeHeroName")
    assertThat(query.queryDocument()).isEqualTo("query EpisodeHeroName(\$episode: Episode) { hero(episode: \$episode) { name } }")
    assertThat(query.variables().marshal()).isEqualTo("{\"episode\":\"EMPIRE\"}")
  }

  @Throws(IOException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }

  @Throws(Exception::class)
  private fun <D : Operation.Data> enqueueCall(call: ApolloQueryCall<D>): List<ApolloCall.StatusEvent?> {
    val statusEvents: MutableList<ApolloCall.StatusEvent?> = ArrayList()
    call.enqueue(object : ApolloCall.Callback<D>() {
      override fun onResponse(response: Response<D>) {}
      override fun onFailure(e: ApolloException) {}
      override fun onStatusEvent(event: ApolloCall.StatusEvent) {
        statusEvents.add(event)
      }
    })
    return statusEvents
  }

  companion object {
    private fun <D : Operation.Data> assertResponse(call: ApolloCall<D>, block: (Response<D>) -> Unit) {
      val response = runBlocking {
        call.await()
      }
      block(response)
    }
  }
}
