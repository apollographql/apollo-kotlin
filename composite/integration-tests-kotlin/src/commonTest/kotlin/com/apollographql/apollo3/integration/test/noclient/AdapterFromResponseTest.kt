package com.apollographql.apollo3.integration.test.runtime

import com.apollographql.apollo3.adapters.LocalDateResponseAdapter
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.use
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery.Data.AllPlanets.Planet.Companion.planetFragment
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery.Data.AllPlanets.Planet.FilmConnection.Film.Companion.filmFragment
import com.apollographql.apollo3.integration.httpcache.fragment.PlanetFragment
import com.apollographql.apollo3.integration.httpcache.type.Types
import com.apollographql.apollo3.integration.normalizer.CharacterDetailsQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.integration.readResource
import kotlinx.datetime.LocalDate
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A series of tests against StreamResponseParser and the generated parsers
 */
class AdapterFromResponseTest {

  @Test
  @Throws(Exception::class)
  fun `errors are properly read`() {
    val response = AllPlanetsQuery().fromResponse(readResource("ResponseError.json"))
    assertTrue(response.hasErrors())
    val errors = response.errors
    assertEquals(errors?.get(0)?.message, "Cannot query field \"names\" on type \"Species\".")
    assertEquals(errors?.get(0)?.locations?.get(0)?.line, 3)
    assertEquals(errors?.get(0)?.locations?.get(0)?.column, 5)
    assertEquals(errors?.get(0)?.customAttributes?.size, 0)
  }

  @Test
  @Throws(Exception::class)
  fun `error with no message, no location and custom attributes`() {
    val response = AllPlanetsQuery().fromResponse(readResource("ResponseErrorWithNullsAndCustomAttributes.json"))
    assertTrue(response.hasErrors())
    assertEquals(response.errors?.size, 1)
    assertEquals(response.errors!![0].message, "")
    assertEquals(response.errors!![0].customAttributes.size, 2)
    assertEquals(response.errors!![0].customAttributes["code"], "userNotFound")
    assertEquals(response.errors!![0].customAttributes["path"], "loginWithPassword")
    assertEquals(response.errors!![0].locations.size, 0)
  }

  @Test
  @Throws(Exception::class)
  fun `error with message, location and custom attributes`() {
    val response = AllPlanetsQuery().fromResponse(readResource("ResponseErrorWithCustomAttributes.json"))
    assertTrue(response.hasErrors())
    assertEquals(response.errors!![0].customAttributes.size, 4)
    assertEquals(response.errors!![0].customAttributes["code"], 500)
    assertEquals(response.errors!![0].customAttributes["status"], "Internal Error")
    assertEquals(response.errors!![0].customAttributes["fatal"], true)
    assertEquals(response.errors!![0].customAttributes["path"], listOf("query"))
  }

  @Test
  @Throws(Exception::class)
  fun errorResponse_with_data() {
    val response = EpisodeHeroNameQuery(Input.Present(Episode.JEDI)).fromResponse(readResource("ResponseErrorWithData.json"))
    val data = response.data
    val errors = response.errors
    assertTrue(data != null)
    assertEquals(data.hero?.name, "R2-D2")
    assertEquals(errors?.size, 1)
    assertEquals(errors?.get(0)?.message, "Cannot query field \"names\" on type \"Species\".")
    assertEquals(errors?.get(0)?.locations?.get(0)?.line, 3)
    assertEquals(errors?.get(0)?.locations?.get(0)?.column, 5)
    assertEquals(errors?.get(0)?.customAttributes?.size, 0)
  }

  private fun <T> ResponseAdapter<T>.toJsonString(t: T): String {
    val buffer = Buffer()
    BufferedSinkJsonWriter(buffer).use {
      toResponse(it, ResponseAdapterCache.DEFAULT, t)
    }
    return buffer.readUtf8()
  }

  @Test
  @Throws(Exception::class)
  fun allFilmsWithDate() {

    val response = AllFilmsQuery().fromResponse(
        readResource("HttpCacheTestAllFilms.json"),
        ResponseAdapterCache(mapOf(Types.Date.name to LocalDateResponseAdapter))
    )
    assertFalse(response.hasErrors())
    assertEquals(response.data!!.allFilms?.films?.size, 6)
    assertEquals(
        response.data!!.allFilms?.films?.map { LocalDateResponseAdapter.toJsonString(it!!.releaseDate) },
        listOf("1977-05-25", "1980-05-17", "1983-05-25", "1999-05-19", "2002-05-16", "2005-05-19").map { "\"$it\"" }
    )
  }

  @Test
  @Throws(Exception::class)
  fun dataNull() {
    val response = HeroNameQuery().fromResponse(readResource("ResponseDataNull.json"))
    assertTrue(response.data == null)
    assertFalse(response.hasErrors())
  }

  @Test
  @Throws(Exception::class)
  fun fieldMissing() {
    try {
      HeroNameQuery().fromResponse(readResource("ResponseDataMissing.json"))
      error("an error was expected")
    } catch (e: NullPointerException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseParser() {
    val data = HeroNameQuery().fromResponse(readResource("HeroNameResponse.json")).data
    assertEquals(data!!.hero?.name, "R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun operationJsonWriter() {
    val expected = readResource("OperationJsonWriter.json")
    val query = AllPlanetsQuery()
    val data = query.fromResponse(expected).data
    val actual = query.toJson(data!!, "  ")
    assertEquals(actual, expected)
  }

  @Test
  @Throws(Exception::class)
  fun parseSuccessOperationRawResponse() {
    val query = AllPlanetsQuery()
    val response = query.fromResponse(readResource("AllPlanetsNullableField.json"))
    assertEquals(response.operation, query)
    assertFalse(response.hasErrors())
    assertTrue(response.data != null)
    assertTrue(response.data!!.allPlanets?.planets?.size != 0)
  }

  @Test
  @Throws(Exception::class)
  fun parseErrorOperationRawResponse() {
    val response = EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE)).fromResponse(
        readResource("/ResponseErrorWithData.json"),
        ResponseAdapterCache.DEFAULT
    )
    val data = response.data
    val errors = response.errors

    assertTrue(data != null)
    assertTrue(data.hero != null)
    assertEquals(data.hero.name, "R2-D2")
    assertEquals(errors?.get(0)?.message, "Cannot query field \"names\" on type \"Species\".")
    assertEquals(errors?.get(0)?.locations?.get(0)?.line, 3)
    assertEquals(errors?.get(0)?.locations?.get(0)?.column, 5)
    assertEquals(errors?.get(0)?.customAttributes?.size, 0)
  }

  @Test
  @Throws(Exception::class)
  fun `extensions are read from response`() {
    val query = HeroNameQuery()
    val extensions = query.fromResponse(readResource("HeroNameResponse.json")).extensions
    assertEquals(
        extensions,
        mapOf(
            "cost" to mapOf(
                "requestedQueryCost" to 3,
                "actualQueryCost" to 3,
                "throttleStatus" to mapOf(
                    "maximumAvailable" to 1000,
                    "currentlyAvailable" to 997,
                    "restoreRate" to 50
                )
            )
        )
    )
  }

  @Test
  fun `not registering an adapter, neither at runtime or in the gradle plugin defaults to Any`() {
    val data = GetJsonScalarQuery.Data(
        json = mapOf("1" to "2", "3" to listOf("a", "b"))
    )
    val query = GetJsonScalarQuery()
    val response = query.fromResponse(query.toJson(data))

    assertEquals(response.data, data)
  }

  /**
   * Nothing really specific here, it's just a bigger response
   */
  @Test
  fun allPlanetQuery() {
    val data = AllPlanetsQuery().fromResponse(readResource("HttpCacheTestAllPlanets.json")).data

    assertEquals(data!!.allPlanets?.planets?.size, 60)
    val planets = data.allPlanets?.planets?.mapNotNull {
      (it as PlanetFragment).name
    }
    assertEquals(planets, ("Tatooine, Alderaan, Yavin IV, Hoth, Dagobah, Bespin, Endor, Naboo, "
        + "Coruscant, Kamino, Geonosis, Utapau, Mustafar, Kashyyyk, Polis Massa, Mygeeto, Felucia, Cato Neimoidia, "
        + "Saleucami, Stewjon, Eriadu, Corellia, Rodia, Nal Hutta, Dantooine, Bestine IV, Ord Mantell, unknown, "
        + "Trandosha, Socorro, Mon Cala, Chandrila, Sullust, Toydaria, Malastare, Dathomir, Ryloth, Aleen Minor, "
        + "Vulpter, Troiken, Tund, Haruun Kal, Cerea, Glee Anselm, Iridonia, Tholoth, Iktotch, Quermia, Dorin, "
        + "Champala, Mirial, Serenno, Concord Dawn, Zolan, Ojom, Skako, Muunilinst, Shili, Kalee, Umbara")
        .split(",")
        .map { it.trim() }
    )
    val firstPlanet = data.allPlanets?.planets?.get(0)
    assertEquals(firstPlanet?.planetFragment()?.climates, listOf("arid"))
    assertEquals(firstPlanet?.planetFragment()?.surfaceWater, 1.0)
    assertEquals(firstPlanet?.filmConnection?.totalCount, 5)
    assertEquals(firstPlanet?.filmConnection?.films?.size, 5)
    assertEquals(firstPlanet?.filmConnection?.films?.get(0)?.filmFragment()?.title, "A New Hope")
    assertEquals(firstPlanet?.filmConnection?.films?.get(0)?.filmFragment()?.producers, listOf("Gary Kurtz", "Rick McCallum"))
  }

  @Test
  fun `forgetting to add a runtime adapter for a scalar registered in the plugin fails`() {
    val data = CharacterDetailsQuery.Data(
        CharacterDetailsQuery.Data.HumanCharacter(
            __typename = "Human",
            name = "Luke",
            birthDate = LocalDate(1970, 1, 1),
            appearsIn = emptyList(),
            firstAppearsIn = Episode.EMPIRE
        )
    )
    val query = CharacterDetailsQuery(id = "1")
    try {
      query.fromResponse(query.toJson(data))
      error("expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("Can't map GraphQL type: `Date`"))
    }
  }
}
