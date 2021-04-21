package com.apollographql.apollo3.integration.test

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.use
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.LocalDateResponseAdapter
import com.apollographql.apollo3.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.httpcache.type.CustomScalars
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.integration.readResource
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A series of tests against StreamResponseParser and the generated parsers
 */
class ResponseParserTest {

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
        ResponseAdapterCache(mapOf(CustomScalars.Date to LocalDateResponseAdapter))
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
}
