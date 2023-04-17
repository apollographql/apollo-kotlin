package test

import assertEquals2
import com.apollographql.apollo3.adapter.KotlinxLocalDateAdapter
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.fromJson
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.api.toJsonString
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.httpcache.type.Date
import com.apollographql.apollo3.integration.normalizer.CharacterWithBirthDateQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import kotlinx.datetime.LocalDate
import okio.Buffer
import testFixtureToJsonReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A series of tests against StreamResponseParser and the generated parsers
 */
class ParseResponseBodyTest {

  @Test
  @Throws(Exception::class)
  fun errorsAreProperlyRead() {
    val response = AllPlanetsQuery().parseJsonResponse(testFixtureToJsonReader("ResponseError.json"))
    assertTrue(response.hasErrors())
    val errors = response.errors
    assertEquals2(errors?.get(0)?.message, "Cannot query field \"names\" on type \"Species\".")
    assertEquals2(errors?.get(0)?.locations?.size, 1)
    assertEquals2(errors?.get(0)?.locations?.get(0)?.line, 3)
    assertEquals2(errors?.get(0)?.locations?.get(0)?.column, 5)
    assertNull(errors?.get(0)?.path)
    assertNull(errors?.get(0)?.extensions)
  }

  @Test
  @Throws(Exception::class)
  fun errorWithNulls() {
    /**
     * If I'm reading the spec right, passing null in location/path/extensions is most likely
     * an error, but we are lenient there and allow it
     */
    val response = AllPlanetsQuery().parseJsonResponse(testFixtureToJsonReader("ResponseErrorWithNulls.json"))
    assertTrue(response.hasErrors())
    assertEquals(response.errors?.size, 1)
    assertEquals(response.errors!![0].message, "Response with nulls")
    assertNull(response.errors!![0].locations)
    assertNull(response.errors!![0].path)
    assertNull(response.errors!![0].extensions)
  }

  @Test
  @Throws(Exception::class)
  fun errorWithAbsent() {
    /**
     * location, path and extensions are all optional
     */
    val response = AllPlanetsQuery().parseJsonResponse(testFixtureToJsonReader("ResponseErrorWithAbsent.json"))
    assertTrue(response.hasErrors())
    assertEquals(response.errors?.size, 1)
    assertEquals(response.errors!![0].message, "Response with absent")
    assertNull(response.errors!![0].locations)
    assertNull(response.errors!![0].path)
    assertNull(response.errors!![0].extensions)
  }


  @Test
  @Throws(Exception::class)
  fun errorWithExtensions() {
    /**
     * Extensions are mapped to Kotlin types.
     * Big numbers should throw although this is not tested here
     */
    val response = AllPlanetsQuery().parseJsonResponse(testFixtureToJsonReader("ResponseErrorWithExtensions.json"))
    assertTrue(response.hasErrors())
    assertEquals(response.errors!![0].extensions?.size, 4)
    assertEquals(response.errors!![0].extensions?.get("code"), 500)
    assertEquals(response.errors!![0].extensions?.get("status"), "Internal Error")
    assertEquals(response.errors!![0].extensions?.get("fatal"), true)
    @Suppress("UNCHECKED_CAST")
    val listOfValues = response.errors!![0].extensions?.get("listOfValues") as List<Any>
    assertEquals(listOfValues[0], 0)
    assertEquals(listOfValues[1], "a")
    assertEquals(listOfValues[2], true)
    assertEquals(listOfValues[3], 2.4)
  }

  @Test
  @Throws(Exception::class)
  fun errorWithNonStandardFields() {
    val response = AllPlanetsQuery().parseJsonResponse(testFixtureToJsonReader("ResponseErrorWithNonStandardFields.json"))
    assertTrue(response.hasErrors())
    val nonStandardFields = response.errors!![0].nonStandardFields!!
    assertEquals(3, nonStandardFields.size)
    assertEquals("INTERNAL_ERROR", nonStandardFields["type"])
    assertNull(nonStandardFields["retry"])

    @Suppress("UNCHECKED_CAST")
    val moreInfo = nonStandardFields["moreInfo"] as Map<String, Any?>
    assertEquals(500, moreInfo["code"])
    assertEquals("Internal Error", moreInfo["status"])
    assertEquals(true, moreInfo["fatal"])

    @Suppress("UNCHECKED_CAST")
    val listOfValues = moreInfo["listOfValues"] as List<Any>
    assertEquals(0, listOfValues[0])
    assertEquals("a", listOfValues[1])
    assertEquals(true, listOfValues[2])
    assertEquals(2.4, listOfValues[3])
  }

  @Test
  @Throws(Exception::class)
  fun errorResponse_with_data() {
    val response = EpisodeHeroNameQuery(Episode.JEDI).parseJsonResponse(testFixtureToJsonReader("ResponseErrorWithData.json"))
    val data = response.data
    val errors = response.errors
    assertTrue(data != null)
    assertEquals(data.hero?.name, "R2-D2")
    assertEquals(errors?.size, 1)
    assertEquals(errors?.get(0)?.message, "Cannot query field \"names\" on type \"Species\".")
    assertEquals(errors?.get(0)?.locations?.get(0)?.line, 3)
    assertEquals(errors?.get(0)?.locations?.get(0)?.column, 5)
    assertEquals(errors?.get(0)?.extensions, null)
  }


  @Test
  @Throws(Exception::class)
  fun allFilmsWithDate() {

    val response = AllFilmsQuery().parseJsonResponse(
        testFixtureToJsonReader("HttpCacheTestAllFilms.json"),
        ScalarAdapters.Builder().add(Date.type, KotlinxLocalDateAdapter).build()
    )
    assertFalse(response.hasErrors())
    assertEquals(response.data!!.allFilms?.films?.size, 6)
    assertEquals(
        response.data!!.allFilms?.films?.map { KotlinxLocalDateAdapter.toJsonString(it!!.releaseDate) },
        listOf("1977-05-25", "1980-05-17", "1983-05-25", "1999-05-19", "2002-05-16", "2005-05-19").map { "\"$it\"" }
    )
  }

  @Test
  @Throws(Exception::class)
  fun dataNull() {
    val response = HeroNameQuery().parseJsonResponse(testFixtureToJsonReader("ResponseDataNull.json"))
    assertTrue(response.data == null)
    assertFalse(response.hasErrors())
  }

  @Test
  @Throws(Exception::class)
  fun fieldMissing() {
    try {
      HeroNameQuery().parseJsonResponse(testFixtureToJsonReader("ResponseDataMissing.json"))
      error("an error was expected")
    } catch (e: NullPointerException) {
      // This is the Kotlin codegen case
    } catch (e: ApolloException) {
      // This is the (better) Java codegen case
    }
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseParser() {
    val data = HeroNameQuery().parseJsonResponse(testFixtureToJsonReader("HeroNameResponse.json")).data
    assertEquals(data!!.hero?.name, "R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun parseSuccessOperationRawResponse() {
    val query = AllPlanetsQuery()
    val response = query.parseJsonResponse(testFixtureToJsonReader("AllPlanetsNullableField.json"))
    assertEquals(response.operation, query)
    assertFalse(response.hasErrors())
    assertTrue(response.data != null)
    assertTrue(response.data!!.allPlanets?.planets?.size != 0)
  }

  @Test
  @Throws(Exception::class)
  fun parseErrorOperationRawResponse() {
    val response = EpisodeHeroNameQuery(Episode.EMPIRE).parseJsonResponse(
        testFixtureToJsonReader("/ResponseErrorWithData.json"),
        ScalarAdapters.Empty
    )
    val data = response.data
    val errors = response.errors

    assertTrue(data != null)
    assertTrue(data.hero != null)
    assertEquals(data.hero.name, "R2-D2")
    assertEquals(errors?.get(0)?.message, "Cannot query field \"names\" on type \"Species\".")
    assertEquals(errors?.get(0)?.locations?.get(0)?.line, 3)
    assertEquals(errors?.get(0)?.locations?.get(0)?.column, 5)
    assertEquals(errors?.get(0)?.extensions, null)
  }

  @Test
  @Throws(Exception::class)
  fun extensionsAreReadFromResponse() {
    val query = HeroNameQuery()
    val extensions = query.parseJsonResponse(testFixtureToJsonReader("HeroNameResponse.json")).extensions
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
        mapOf("1" to "2", "3" to listOf("a", "b"))
    )
    val query = GetJsonScalarQuery()

    val dataString = query.adapter().toJsonString(data)
    assertEquals(
        query.adapter().fromJson(Buffer().writeUtf8(dataString).jsonReader(), ScalarAdapters.Empty),
        data
    )
  }

  @Test
  fun parseFloats() {
    val dataMap = mapOf("a" to 1.1)
    val data = GetJsonScalarQuery.Data(dataMap)
    val query = GetJsonScalarQuery()
    assertEquals(
        data,
        query.adapter().fromJson(MapJsonReader(mapOf("json" to dataMap)), ScalarAdapters.Empty)
    )
  }

  @Test
  fun forgettingToAddARuntimeAdapterForAScalarRegisteredInThePluginFails() {
    val data = CharacterWithBirthDateQuery.Data(
        CharacterWithBirthDateQuery.Character(
            LocalDate(1970, 1, 1),
        )
    )
    val query = CharacterWithBirthDateQuery("1")
    try {
      val dataString = query.adapter().toJsonString(data)
      query.adapter().fromJson(Buffer().writeUtf8(dataString).jsonReader(), ScalarAdapters.Empty)
      error("expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("Can't map GraphQL type: `Date`"))
    }
  }
}
