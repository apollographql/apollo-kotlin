package com.apollographql.apollo

import com.apollographql.apollo.Utils.checkTestFixture
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.readFileToString
import com.apollographql.apollo.api.CustomScalarAdapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Input.Companion.fromNullable
import com.apollographql.apollo.api.JsonElement
import com.apollographql.apollo.api.JsonString
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.api.toJson
import com.apollographql.apollo.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.http.OkHttpExecutionContext
import com.apollographql.apollo.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.httpcache.fragment.FilmFragment
import com.apollographql.apollo.integration.httpcache.fragment.PlanetFragment
import com.apollographql.apollo.integration.httpcache.type.CustomScalars
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.rx2.Rx2Apollo
import com.google.common.base.Charsets
import com.google.common.truth.Truth.assertThat
import io.reactivex.functions.Predicate
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.Locale

class IntegrationTest {
  private lateinit var apolloClient: ApolloClient
  private var dateCustomScalarAdapter = object : CustomScalarAdapter<Date> {
    override fun decode(jsonElement: JsonElement): Date {
      return try {
        DATE_FORMAT.parse(jsonElement.toRawValue().toString())
      } catch (e: ParseException) {
        throw RuntimeException(e)
      }
    }

    override fun encode(value: Date): JsonElement {
      return JsonString(DATE_FORMAT.format(value))
    }
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
    assertResponse(
        apolloClient.query(AllPlanetsQuery())
    ) {
      true
    }

    val body = server.takeRequest().body.readString(Charsets.UTF_8)
    checkTestFixture(body, "IntegrationTest/allPlanets.json")
  }

  @Test
  @Throws(Exception::class)
  fun error_response() {
    server.enqueue(mockResponse("ResponseError.json"))
    assertResponse(
        apolloClient.query(AllPlanetsQuery()),
        Predicate<Response<AllPlanetsQuery.Data>> { response ->
          assertThat(response.hasErrors()).isTrue()
          assertThat(response.errors).containsExactly(Error(
              "Cannot query field \"names\" on type \"Species\".", listOf(Error.Location(3, 5)), emptyMap<String, Any>()))
          true
        }
    )
  }

  @Test
  @Throws(Exception::class)
  fun error_response_with_nulls_and_custom_attributes() {
    server.enqueue(mockResponse("ResponseErrorWithNullsAndCustomAttributes.json"))
    assertResponse(
        apolloClient.query(AllPlanetsQuery())
    ) { response ->
      assertThat(response.hasErrors()).isTrue()
      assertThat(response.errors).hasSize(1)
      assertThat(response.errors!![0].message).isEqualTo("")
      assertThat(response.errors!![0].customAttributes).hasSize(2)
      assertThat(response.errors!![0].customAttributes["code"]).isEqualTo("userNotFound")
      assertThat(response.errors!![0].customAttributes["path"]).isEqualTo("loginWithPassword")
      assertThat(response.errors!![0].locations).hasSize(0)
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun errorResponse_custom_attributes() {
    server.enqueue(mockResponse("ResponseErrorWithCustomAttributes.json"))
    assertResponse(
        apolloClient.query(AllPlanetsQuery())
    ) { response ->
      assertThat(response.hasErrors()).isTrue()
      assertThat(response.errors!![0].customAttributes).hasSize(4)
      assertThat(response.errors!![0].customAttributes["code"]).isEqualTo(BigDecimal(500))
      assertThat(response.errors!![0].customAttributes["status"]).isEqualTo("Internal Error")
      assertThat(response.errors!![0].customAttributes["fatal"]).isEqualTo(true)
      assertThat(response.errors!![0].customAttributes["path"]).isEqualTo(Arrays.asList("query"))
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun errorResponse_with_data() {
    server.enqueue(mockResponse("ResponseErrorWithData.json"))
    assertResponse(
        apolloClient.query(EpisodeHeroNameQuery(fromNullable(Episode.JEDI)))
    ) { (_, data, errors) ->
      assertThat(data).isNotNull()
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(errors).containsExactly(Error(
          "Cannot query field \"names\" on type \"Species\".", listOf(Error.Location(3, 5)), emptyMap<String, Any>()))
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun allFilmsWithDate() {
    server.enqueue(mockResponse("HttpCacheTestAllFilms.json"))
    assertResponse(
        apolloClient.query(AllFilmsQuery())
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.allFilms?.films).hasSize(6)
      val dates = response.data!!.allFilms?.films?.mapNotNull {
        val releaseDate = it!!.releaseDate
        dateCustomScalarAdapter.encode(releaseDate).toRawValue().toString()
      }
      assertThat(dates).isEqualTo(Arrays.asList("1977-05-25", "1980-05-17", "1983-05-25", "1999-05-19",
          "2002-05-16", "2005-05-19"))
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun dataNull() {
    server.enqueue(mockResponse("ResponseDataNull.json"))
    assertResponse(
        apolloClient.query(HeroNameQuery())
    ) { response ->
      assertThat(response.data).isNull()
      assertThat(response.hasErrors()).isFalse()
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun fieldMissing() {
    server.enqueue(mockResponse("ResponseDataMissing.json"))
    Rx2Apollo.from(apolloClient.query(HeroNameQuery()))
        .test()
        .assertError(ApolloException::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun statusEvents() {
    server.enqueue(mockResponse("HeroNameResponse.json"))
    var statusEvents = enqueueCall(apolloClient.query(HeroNameQuery()))
    assertThat(statusEvents).isEqualTo(Arrays.asList(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent.FETCH_NETWORK, ApolloCall.StatusEvent.COMPLETED))
    statusEvents = enqueueCall(
        apolloClient.query(HeroNameQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY))
    assertThat(statusEvents).isEqualTo(Arrays.asList(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent.FETCH_CACHE, ApolloCall.StatusEvent.COMPLETED))
    server.enqueue(mockResponse("HeroNameResponse.json"))
    statusEvents = enqueueCall(
        apolloClient.query(HeroNameQuery()).responseFetcher(ApolloResponseFetchers.CACHE_AND_NETWORK))
    assertThat(statusEvents).isEqualTo(Arrays.asList(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent.FETCH_CACHE, ApolloCall.StatusEvent.FETCH_NETWORK, ApolloCall.StatusEvent.COMPLETED))
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseParser() {
    val json = readFileToString(javaClass, "/HeroNameResponse.json")
    val query = HeroNameQuery()
    val data = query.parse(json).data
    assertThat(data!!.hero?.name).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun operationJsonWriter() {
    val expected = readFileToString(javaClass, "/OperationJsonWriter.json")
    val query = AllPlanetsQuery()
    val data = query.parse(expected).data
    val actual = query.toJson(data!!, "  ")
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  @Throws(Exception::class)
  fun parseSuccessOperationRawResponse() {
    val query = AllPlanetsQuery()
    val response = query.parse(
        Buffer().readFrom(javaClass.getResourceAsStream("/AllPlanetsNullableField.json")),
        CustomScalarAdapters(emptyMap())
    )
    assertThat(response.operation).isEqualTo(query)
    assertThat(response.hasErrors()).isFalse()
    assertThat(response.data).isNotNull()
    assertThat(response.data!!.allPlanets?.planets).isNotEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun parseErrorOperationRawResponse() {
    val query = EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))
    val (_, data, errors) = query.parse(
        Buffer().readFrom(javaClass.getResourceAsStream("/ResponseErrorWithData.json")),
        CustomScalarAdapters(emptyMap())
    )
    assertThat(data).isNotNull()
    assertThat(data!!.hero).isNotNull()
    assertThat(data.hero?.name).isEqualTo("R2-D2")
    assertThat(errors).containsExactly(
        Error(
            "Cannot query field \"names\" on type \"Species\".", listOf(Error.Location(3, 5)), emptyMap<String, Any>())
    )
  }

  @Test
  @Throws(Exception::class)
  fun writeOperationRawRequest() {
    val query = EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))
    val payload = "{" +
        "\"operationName\": " + query.name() + ", " +
        "\"query\": " + query.queryDocument() + ", " +
        "\"variables\": " + query.variables().marshal() +
        "}"
    assertThat(payload).isEqualTo("{\"operationName\": EpisodeHeroName, \"query\": query EpisodeHeroName(\$episode: Episode) { hero(episode: \$episode) { name } }, \"variables\": {\"episode\":\"EMPIRE\"}}")
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseParserParseResponseWithExtensions() {
    val source = Buffer().readFrom(javaClass.getResourceAsStream("/HeroNameResponse.json"))
    val query = HeroNameQuery()
    val extensions = query.parse(source).extensions
    assertThat(extensions.toString()).isEqualTo("{cost={requestedQueryCost=3, actualQueryCost=3, throttleStatus={maximumAvailable=1000, currentlyAvailable=997, restoreRate=50}}}")
  }

  @Test
  @Throws(Exception::class)
  fun operationParseResponseWithExtensions() {
    val source = Buffer().readFrom(javaClass.getResourceAsStream("/HeroNameResponse.json"))
    val extensions = HeroNameQuery().parse(source).extensions
    assertThat(extensions.toString()).isEqualTo("{cost={requestedQueryCost=3, actualQueryCost=3, throttleStatus={maximumAvailable=1000, currentlyAvailable=997, restoreRate=50}}}")
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseContainsHttpExecutionContext() {
    val httpResponse = mockResponse("HttpCacheTestAllPlanets.json")
        .setHeader("Header1", "Header1#value")
        .setHeader("Header2", "Header2#value")
    server.enqueue(httpResponse)
    assertResponse(
        apolloClient.query(AllPlanetsQuery()),
        Predicate { response: Response<AllPlanetsQuery.Data> ->
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
          true
        }
    )
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
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun <D : Operation.Data> assertResponse(call: ApolloCall<D>, predicate: Predicate<Response<D>>) {
      Rx2Apollo.from(call)
          .test()
          .assertValue(predicate)
    }
  }
}
