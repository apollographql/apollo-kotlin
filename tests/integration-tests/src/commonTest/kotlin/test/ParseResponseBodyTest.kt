package test

import assertEquals2
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.StringAdapter
import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.toApolloResponse
import com.apollographql.apollo.api.toJsonString
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.httpcache.type.Date
import com.apollographql.apollo.integration.normalizer.CharacterWithBirthDateQuery
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
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
    val response = testFixtureToJsonReader("ResponseError.json").toApolloResponse(operation = AllPlanetsQuery())
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
    val response = testFixtureToJsonReader("ResponseErrorWithNulls.json").toApolloResponse(operation = AllPlanetsQuery())
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
    val response = testFixtureToJsonReader("ResponseErrorWithAbsent.json").toApolloResponse(operation = AllPlanetsQuery())
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
    val response = testFixtureToJsonReader("ResponseErrorWithExtensions.json").toApolloResponse(operation = AllPlanetsQuery())
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
    val response = testFixtureToJsonReader("ResponseErrorWithNonStandardFields.json").toApolloResponse(operation = AllPlanetsQuery())
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
    val response = testFixtureToJsonReader("ResponseErrorWithData.json").toApolloResponse(operation = EpisodeHeroNameQuery(Episode.JEDI))
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

    val response = testFixtureToJsonReader("HttpCacheTestAllFilms.json").toApolloResponse(operation = AllFilmsQuery(), customScalarAdapters = CustomScalarAdapters.Builder().add(Date.type, StringAdapter).build())
    assertFalse(response.hasErrors())
    assertEquals(response.data!!.allFilms?.films?.size, 6)
    assertEquals(
        response.data!!.allFilms?.films?.map { StringAdapter.toJsonString(it!!.releaseDate) },
        listOf("1977-05-25", "1980-05-17", "1983-05-25", "1999-05-19", "2002-05-16", "2005-05-19").map { "\"$it\"" }
    )
  }

  @Test
  @Throws(Exception::class)
  fun dataNull() {
    val response = testFixtureToJsonReader("ResponseDataNull.json").toApolloResponse(operation = HeroNameQuery())
    assertTrue(response.data == null)
    assertFalse(response.hasErrors())
  }

  @Test
  @Throws(Exception::class)
  fun fieldMissing() {
    val exception = testFixtureToJsonReader("ResponseDataMissing.json").toApolloResponse(operation = HeroNameQuery()).exception
    if (exception is DefaultApolloException) {
      assertTrue(exception.message?.contains("Field 'name' is missing") == true)
    }
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseParser() {
    val data = testFixtureToJsonReader("HeroNameResponse.json").toApolloResponse(operation = HeroNameQuery()).data
    assertEquals(data!!.hero?.name, "R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun parseSuccessOperationRawResponse() {
    val query = AllPlanetsQuery()
    val response = testFixtureToJsonReader("AllPlanetsNullableField.json").toApolloResponse(operation = query)
    assertEquals(response.operation, query)
    assertFalse(response.hasErrors())
    assertTrue(response.data != null)
    assertTrue(response.data!!.allPlanets?.planets?.size != 0)
  }

  @Test
  @Throws(Exception::class)
  fun parseErrorOperationRawResponse() {
    val response = testFixtureToJsonReader("/ResponseErrorWithData.json").toApolloResponse(operation = EpisodeHeroNameQuery(Episode.EMPIRE), customScalarAdapters = CustomScalarAdapters.Empty)
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
    val extensions = testFixtureToJsonReader("HeroNameResponse.json").toApolloResponse(operation = query).extensions
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
        query.adapter().fromJson(Buffer().writeUtf8(dataString).jsonReader(), CustomScalarAdapters.Empty),
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
        query.adapter().fromJson(MapJsonReader(mapOf("json" to dataMap)), CustomScalarAdapters.Empty)
    )
  }
}
