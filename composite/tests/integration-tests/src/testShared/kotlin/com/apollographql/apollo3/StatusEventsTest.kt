package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.checkTestFixture
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.readFileToString
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.exception.ApolloException
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.variablesJson
import com.apollographql.apollo3.cache.normalized.IdCacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.coroutines.await
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.http.OkHttpExecutionContext
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.integration.normalizer.type.Types
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
import java.util.Date
import java.util.Locale

class IntegrationTest {
  private lateinit var apolloClient: ApolloClient
  private val dateCustomScalarAdapter: Adapter<Date> = object : Adapter<Date> {
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Date {
      return DATE_FORMAT.parse(reader.nextString())
    }

    override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Date) {
      writer.value(DATE_FORMAT.format(value))
    }
  }

  val server = MockWebServer()

  @Before
  fun setUp() {
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(OkHttpClient.Builder().dispatcher(Dispatcher(immediateExecutorService())).build())
        .addCustomScalarAdapter(Types.Date, dateCustomScalarAdapter)
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdCacheResolver())
        .defaultResponseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
        .dispatcher(immediateExecutor())
        .build()
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



  @Throws(IOException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }

  @Throws(Exception::class)
  private fun <D : Operation.Data> enqueueCall(call: ApolloQueryCall<D>): List<ApolloCall.StatusEvent?> {
    val statusEvents: MutableList<ApolloCall.StatusEvent?> = ArrayList()
    call.enqueue(object : ApolloCall.Callback<D>() {
      override fun onResponse(response: ApolloResponse<D>) {}
      override fun onFailure(e: ApolloException) {}
      override fun onStatusEvent(event: ApolloCall.StatusEvent) {
        statusEvents.add(event)
      }
    })
    return statusEvents
  }

  companion object {
    private fun <D : Operation.Data> assertResponse(call: ApolloCall<D>, block: (ApolloResponse<D>) -> Unit) {
      val response = runBlocking {
        call.await()
      }
      block(response)
    }
  }
}
