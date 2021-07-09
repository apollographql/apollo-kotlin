package test

import com.apollographql.apollo3.adapter.LocalDateAdapter
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.fromJson
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.use
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.httpcache.type.Types
import com.apollographql.apollo3.integration.normalizer.CharacterWithBirthDateQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import kotlinx.datetime.LocalDate
import okio.Buffer
import readResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A series of tests against StreamResponseParser and the generated parsers
 */
class ParseResponseBodyTest {

  @Test
  @Throws(Exception::class)
  fun errorsAreProperlyRead() {
    val response = AllPlanetsQuery().parseJsonResponse(readResource("ResponseError.json"))
    assertTrue(response.hasErrors())
    val errors = response.errors
    assertEquals(errors?.get(0)?.message, "Cannot query field \"names\" on type \"Species\".")
    assertEquals(errors?.get(0)?.locations?.get(0)?.line, 3)
    assertEquals(errors?.get(0)?.locations?.get(0)?.column, 5)
    assertEquals(errors?.get(0)?.customAttributes?.size, 0)
  }

  @Test
  @Throws(Exception::class)
  fun errorWithNoMessageNoLocationAndCustomAttributes() {
    val response = AllPlanetsQuery().parseJsonResponse(readResource("ResponseErrorWithNullsAndCustomAttributes.json"))
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
  fun errorWithMessageLocationAndCustomAttributes() {
    val response = AllPlanetsQuery().parseJsonResponse(readResource("ResponseErrorWithCustomAttributes.json"))
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
    val response = EpisodeHeroNameQuery(Episode.JEDI).parseJsonResponse(readResource("ResponseErrorWithData.json"))
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

  private fun <T> Adapter<T>.toJsonString(t: T): String {
    val buffer = Buffer()
    BufferedSinkJsonWriter(buffer).use {
      toJson(it, CustomScalarAdapters.Empty, t)
    }
    return buffer.readUtf8()
  }

  @Test
  @Throws(Exception::class)
  fun allFilmsWithDate() {

    val response = AllFilmsQuery().parseJsonResponse(
        readResource("HttpCacheTestAllFilms.json"),
        CustomScalarAdapters(mapOf(Types.Date.name to LocalDateAdapter))
    )
    assertFalse(response.hasErrors())
    assertEquals(response.data!!.allFilms?.films?.size, 6)
    assertEquals(
        response.data!!.allFilms?.films?.map { LocalDateAdapter.toJsonString(it!!.releaseDate) },
        listOf("1977-05-25", "1980-05-17", "1983-05-25", "1999-05-19", "2002-05-16", "2005-05-19").map { "\"$it\"" }
    )
  }

  @Test
  @Throws(Exception::class)
  fun dataNull() {
    val response = HeroNameQuery().parseJsonResponse(readResource("ResponseDataNull.json"))
    assertTrue(response.data == null)
    assertFalse(response.hasErrors())
  }

  @Test
  @Throws(Exception::class)
  fun fieldMissing() {
    try {
      HeroNameQuery().parseJsonResponse(readResource("ResponseDataMissing.json"))
      error("an error was expected")
    } catch (e: NullPointerException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseParser() {
    val data = HeroNameQuery().parseJsonResponse(readResource("HeroNameResponse.json")).data
    assertEquals(data!!.hero?.name, "R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun parseSuccessOperationRawResponse() {
    val query = AllPlanetsQuery()
    val response = query.parseJsonResponse(readResource("AllPlanetsNullableField.json"))
    assertEquals(response.operation, query)
    assertFalse(response.hasErrors())
    assertTrue(response.data != null)
    assertTrue(response.data!!.allPlanets?.planets?.size != 0)
  }

  @Test
  @Throws(Exception::class)
  fun parseErrorOperationRawResponse() {
    val response = EpisodeHeroNameQuery(Episode.EMPIRE).parseJsonResponse(
        readResource("/ResponseErrorWithData.json"),
        CustomScalarAdapters.Empty
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
  fun extensionsAreReadFromResponse() {
    val query = HeroNameQuery()
    val extensions = query.parseJsonResponse(readResource("HeroNameResponse.json")).extensions
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
  fun notRegisteringAnAdapterNeitherAtRuntimeOrInTheGradlePluginDefaultsToAny() {
    val data = GetJsonScalarQuery.Data(
        json = mapOf("1" to "2", "3" to listOf("a", "b"))
    )
    val query = GetJsonScalarQuery()

    assertEquals(query.adapter().fromJson(query.adapter().toJson(data)), data)
  }


  @Test
  fun forgettingToAddARuntimeAdapterForAScalarRegisteredInThePluginFails() {
    val data = CharacterWithBirthDateQuery.Data(
        character = CharacterWithBirthDateQuery.Data.Character(
            birthDate = LocalDate(1970, 1, 1),
        )
    )
    val query = CharacterWithBirthDateQuery(id = "1")
    try {
      query.adapter().fromJson(query.adapter().toJson(data))
      error("expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("Can't map GraphQL type: `Date`"))
    }
  }
}
