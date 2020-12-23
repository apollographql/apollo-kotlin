package com.apollographql.apollo

import com.apollographql.apollo.Utils.checkTestFixture
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.readFileToString
import com.apollographql.apollo.api.*
import com.apollographql.apollo.api.JsonElement.JsonString
import com.apollographql.apollo.api.Input.Companion.fromNullable
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.http.OkHttpExecutionContext
import com.apollographql.apollo.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery.Data.AllPlanet.Planet
import com.apollographql.apollo.integration.httpcache.fragment.FilmFragment
import com.apollographql.apollo.integration.httpcache.fragment.PlanetFragment
import com.apollographql.apollo.integration.httpcache.type.CustomScalarType
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.response.OperationResponseParser
import com.apollographql.apollo.rx2.Rx2Apollo
import com.google.common.base.Charsets
import com.google.common.base.Function
import com.google.common.collect.FluentIterable
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.reactivex.functions.Predicate
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class IntegrationTest {
  private lateinit var apolloClient: ApolloClient
  private lateinit var dateCustomScalarTypeAdapter: CustomScalarTypeAdapter<Date>

  val server = MockWebServer()

  @Before
  fun setUp() {
    dateCustomScalarTypeAdapter = object : CustomScalarTypeAdapter<Date> {
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
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(OkHttpClient.Builder().dispatcher(Dispatcher(immediateExecutorService())).build())
        .addCustomScalarTypeAdapter(CustomScalarType.Date, dateCustomScalarTypeAdapter)
        .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdFieldCacheKeyResolver())
        .defaultResponseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
        .dispatcher(immediateExecutor())
        .build()
  }

  @Test
  @Throws(Exception::class)
  fun allPlanetQuery() {
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"))
    assertResponse(
        apolloClient!!.query(AllPlanetsQuery())
    ) { (_, data) ->
      assertThat(data!!.allPlanets?.planets?.size).isEqualTo(60)
      val planets = data!!.allPlanets?.planets?.mapNotNull {
        (it as PlanetFragment).name
      }
      assertThat(planets).isEqualTo(("Tatooine, Alderaan, Yavin IV, Hoth, Dagobah, Bespin, Endor, Naboo, "
          + "Coruscant, Kamino, Geonosis, Utapau, Mustafar, Kashyyyk, Polis Massa, Mygeeto, Felucia, Cato Neimoidia, "
          + "Saleucami, Stewjon, Eriadu, Corellia, Rodia, Nal Hutta, Dantooine, Bestine IV, Ord Mantell, unknown, "
          + "Trandosha, Socorro, Mon Cala, Chandrila, Sullust, Toydaria, Malastare, Dathomir, Ryloth, Aleen Minor, "
          + "Vulpter, Troiken, Tund, Haruun Kal, Cerea, Glee Anselm, Iridonia, Tholoth, Iktotch, Quermia, Dorin, "
          + "Champala, Mirial, Serenno, Concord Dawn, Zolan, Ojom, Skako, Muunilinst, Shili, Kalee, Umbara")
          .split(",")
          .map { it.trim() }
      )
      val firstPlanet = data!!.allPlanets?.planets?.get(0)
      assertThat((firstPlanet as PlanetFragment).climates).isEqualTo(listOf("arid"))
      assertThat((firstPlanet as PlanetFragment).surfaceWater).isWithin(1.0)
      assertThat(firstPlanet.filmConnection?.totalCount).isEqualTo(5)
      assertThat(firstPlanet.filmConnection?.films?.size).isEqualTo(5)
      assertThat((firstPlanet.filmConnection?.films?.get(0) as FilmFragment)?.title).isEqualTo("A New Hope")
      assertThat((firstPlanet.filmConnection?.films?.get(0) as FilmFragment)?.producers).isEqualTo(listOf("Gary Kurtz", "Rick McCallum"))
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
        apolloClient!!.query(AllPlanetsQuery()),
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
        apolloClient!!.query(AllPlanetsQuery())
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
        apolloClient!!.query(AllPlanetsQuery())
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
        apolloClient!!.query(EpisodeHeroNameQuery(fromNullable(Episode.JEDI)))
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
        apolloClient!!.query(AllFilmsQuery())
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.allFilms?.films).hasSize(6)
      val dates = response.data!!.allFilms?.films?.mapNotNull {
        val releaseDate = it!!.releaseDate!!
        dateCustomScalarTypeAdapter!!.encode(releaseDate).toRawValue().toString()
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
        apolloClient!!.query(HeroNameQuery()),
        Predicate<Response<HeroNameQuery.Data>> { response ->
          assertThat(response.data).isNull()
          assertThat(response.hasErrors()).isFalse()
          true
        }
    )
  }

  @Test
  @Throws(Exception::class)
  fun fieldMissing() {
    server.enqueue(mockResponse("ResponseDataMissing.json"))
    Rx2Apollo.from(apolloClient!!.query(HeroNameQuery()))
        .test()
        .assertError(ApolloException::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun statusEvents() {
    server.enqueue(mockResponse("HeroNameResponse.json"))
    var statusEvents = enqueueCall(apolloClient!!.query(HeroNameQuery()))
    assertThat(statusEvents).isEqualTo(Arrays.asList(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent.FETCH_NETWORK, ApolloCall.StatusEvent.COMPLETED))
    statusEvents = enqueueCall(
        apolloClient!!.query(HeroNameQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY))
    assertThat(statusEvents).isEqualTo(Arrays.asList(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent.FETCH_CACHE, ApolloCall.StatusEvent.COMPLETED))
    server.enqueue(mockResponse("HeroNameResponse.json"))
    statusEvents = enqueueCall(
        apolloClient!!.query(HeroNameQuery()).responseFetcher(ApolloResponseFetchers.CACHE_AND_NETWORK))
    assertThat(statusEvents).isEqualTo(Arrays.asList(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent.FETCH_CACHE, ApolloCall.StatusEvent.FETCH_NETWORK, ApolloCall.StatusEvent.COMPLETED))
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseParser() {
    val json = readFileToString(javaClass, "/HeroNameResponse.json")
    val query = HeroNameQuery()
    val (_, data) = OperationResponseParser(query, query.responseFieldMapper(), ScalarTypeAdapters(emptyMap()))
        .parse(Buffer().writeUtf8(json))
    assertThat(data!!.hero?.name).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun operationJsonWriter() {
    val expected = readFileToString(javaClass, "/OperationJsonWriter.json")
    val query = AllPlanetsQuery()
    val (_, data) = OperationResponseParser(query, query.responseFieldMapper(), ScalarTypeAdapters.DEFAULT)
        .parse(Buffer().writeUtf8(expected))
    val actual = data!!.toJson("  ")
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  @Throws(Exception::class)
  fun parseSuccessOperationRawResponse() {
    val query = AllPlanetsQuery()
    val response = query.parse(
        Buffer().readFrom(javaClass.getResourceAsStream("/AllPlanetsNullableField.json")),
        ScalarTypeAdapters(emptyMap())
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
        ScalarTypeAdapters(emptyMap())
    )
    assertThat(data).isNotNull()
    assertThat(data!!.hero).isNotNull()
    assertThat(data!!.hero?.name).isEqualTo("R2-D2")
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
        "\"operationName\": " + query.name().name() + ", " +
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
    val (_, _, _, _, _, extensions) = OperationResponseParser(query, query.responseFieldMapper(),
        ScalarTypeAdapters(emptyMap())).parse(source)
    assertThat(extensions.toString()).isEqualTo("{cost={requestedQueryCost=3, actualQueryCost=3, throttleStatus={maximumAvailable=1000, currentlyAvailable=997, restoreRate=50}}}")
  }

  @Test
  @Throws(Exception::class)
  fun operationParseResponseWithExtensions() {
    val source = Buffer().readFrom(javaClass.getResourceAsStream("/HeroNameResponse.json"))
    val (_, _, _, _, _, extensions) = HeroNameQuery().parse(source)
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
        apolloClient!!.query(AllPlanetsQuery()),
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
        } as Predicate<Response<AllPlanetsQuery.Data>>
    )
  }

  @Throws(IOException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }

  @Throws(Exception::class)
  private fun <D: Operation.Data> enqueueCall(call: ApolloQueryCall<D>): List<ApolloCall.StatusEvent?> {
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
    private fun <D: Operation.Data> assertResponse(call: ApolloCall<D>, predicate: Predicate<Response<D>>) {
      Rx2Apollo.from(call)
          .test()
          .assertValue(predicate)
    }
  }
}
