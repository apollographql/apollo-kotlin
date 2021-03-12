package com.apollographql.apollo3

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.use
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.httpcache.type.CustomScalars
import com.apollographql.apollo3.integration.normalizer.CharacterDetailsQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.google.common.truth.Truth.assertThat
import okio.Buffer
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A series of tests against StreamResponseParser and the generated parsers
 */
class ResponseParserTest {

  @Test
  @Throws(Exception::class)
  fun `errors are properly read`() {
    val response = AllPlanetsQuery().fromResponse(Utils.readResource("ResponseError.json"))
    assertThat(response.hasErrors()).isTrue()
    val errors = response.errors
    assertThat(errors?.get(0)?.message).isEqualTo("Cannot query field \"names\" on type \"Species\".")
    assertThat(errors?.get(0)?.locations?.get(0)?.line).isEqualTo(3)
    assertThat(errors?.get(0)?.locations?.get(0)?.column).isEqualTo(5)
    assertThat(errors?.get(0)?.customAttributes?.size).isEqualTo(0)
  }

  @Test
  @Throws(Exception::class)
  fun `error with no message, no location and custom attributes`() {
    val response = AllPlanetsQuery().fromResponse(Utils.readResource("ResponseErrorWithNullsAndCustomAttributes.json"))
    assertThat(response.hasErrors()).isTrue()
    assertThat(response.errors).hasSize(1)
    assertThat(response.errors!![0].message).isEqualTo("")
    assertThat(response.errors!![0].customAttributes).hasSize(2)
    assertThat(response.errors!![0].customAttributes["code"]).isEqualTo("userNotFound")
    assertThat(response.errors!![0].customAttributes["path"]).isEqualTo("loginWithPassword")
    assertThat(response.errors!![0].locations).hasSize(0)
  }

  @Test
  @Throws(Exception::class)
  fun `error with message, location and custom attributes`() {
    val response = AllPlanetsQuery().fromResponse(Utils.readResource("ResponseErrorWithCustomAttributes.json"))
    assertThat(response.hasErrors()).isTrue()
    assertThat(response.errors!![0].customAttributes).hasSize(4)
    assertThat(response.errors!![0].customAttributes["code"]).isEqualTo(500)
    assertThat(response.errors!![0].customAttributes["status"]).isEqualTo("Internal Error")
    assertThat(response.errors!![0].customAttributes["fatal"]).isEqualTo(true)
    assertThat(response.errors!![0].customAttributes["path"]).isEqualTo(listOf("query"))
  }

  @Test
  @Throws(Exception::class)
  fun errorResponse_with_data() {
    val response = EpisodeHeroNameQuery(Input.Present(Episode.JEDI)).fromResponse(Utils.readResource("ResponseErrorWithData.json"))
    val data = response.data
    val errors = response.errors
    assertThat(data).isNotNull()
    assertThat(data!!.hero?.name).isEqualTo("R2-D2")
    assertThat(errors?.size).isEqualTo(1)
    assertThat(errors?.get(0)?.message).isEqualTo("Cannot query field \"names\" on type \"Species\".")
    assertThat(errors?.get(0)?.locations?.get(0)?.line).isEqualTo(3)
    assertThat(errors?.get(0)?.locations?.get(0)?.column).isEqualTo(5)
    assertThat(errors?.get(0)?.customAttributes?.size).isEqualTo(0)
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
    val dateCustomScalarAdapter = object : ResponseAdapter<Date> {
      private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      override fun fromResponse(reader: JsonReader, responseAdapterCache: ResponseAdapterCache): Date {
        return  DATE_FORMAT.parse(reader.nextString())
      }

      override fun toResponse(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache, value: Date) {
        writer.value(DATE_FORMAT.format(value))
      }
    }
    val response = AllFilmsQuery().fromResponse(Utils.readResource("HttpCacheTestAllFilms.json"), ResponseAdapterCache(mapOf(CustomScalars.Date to dateCustomScalarAdapter)))
    assertThat(response.hasErrors()).isFalse()
    assertThat(response.data!!.allFilms?.films).hasSize(6)
    assertThat(response.data!!.allFilms?.films?.map { dateCustomScalarAdapter.toJsonString(it!!.releaseDate) }).isEqualTo(
        listOf("1977-05-25", "1980-05-17", "1983-05-25", "1999-05-19", "2002-05-16", "2005-05-19").map { "\"$it\"" }
    )
  }

  @Test
  @Throws(Exception::class)
  fun dataNull() {
    val response = HeroNameQuery().fromResponse(Utils.readResource("ResponseDataNull.json"))
    assertThat(response.data).isNull()
    assertThat(response.hasErrors()).isFalse()
  }

  @Test
  @Throws(Exception::class)
  fun fieldMissing() {
    try {
      HeroNameQuery().fromResponse(Utils.readResource("ResponseDataMissing.json"))
      error("an error was expected")
    } catch (e: NullPointerException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseParser() {
    val data = HeroNameQuery().fromResponse(Utils.readResource("HeroNameResponse.json")).data
    assertThat(data!!.hero?.name).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun operationJsonWriter() {
    val expected = Utils.readResource("OperationJsonWriter.json")
    val query = AllPlanetsQuery()
    val data = query.fromResponse(expected).data
    val actual = query.toJson(data!!, "  ")
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  @Throws(Exception::class)
  fun parseSuccessOperationRawResponse() {
    val query = AllPlanetsQuery()
    val response = query.fromResponse(Utils.readResource("AllPlanetsNullableField.json"))
    assertThat(response.operation).isEqualTo(query)
    assertThat(response.hasErrors()).isFalse()
    assertThat(response.data).isNotNull()
    assertThat(response.data!!.allPlanets?.planets).isNotEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun parseErrorOperationRawResponse() {
    val response = EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE)).fromResponse(
        Utils.readResource("/ResponseErrorWithData.json"),
        ResponseAdapterCache.DEFAULT
    )
    val data = response.data
    val errors = response.errors

    assertThat(data).isNotNull()
    assertThat(data!!.hero).isNotNull()
    assertThat(data.hero?.name).isEqualTo("R2-D2")
    assertThat(errors?.get(0)?.message).isEqualTo("Cannot query field \"names\" on type \"Species\".")
    assertThat(errors?.get(0)?.locations?.get(0)?.line).isEqualTo(3)
    assertThat(errors?.get(0)?.locations?.get(0)?.column).isEqualTo(5)
    assertThat(errors?.get(0)?.customAttributes?.size).isEqualTo(0)
  }

  @Test
  @Throws(Exception::class)
  fun `extensions are read from response`() {
    val query = HeroNameQuery()
    val extensions = query.fromResponse(Utils.readResource("HeroNameResponse.json")).extensions
    assertThat(extensions).isEqualTo(mapOf(
        "cost" to mapOf(
            "requestedQueryCost" to 3,
            "actualQueryCost" to 3,
            "throttleStatus" to mapOf(
                "maximumAvailable" to 1000,
                "currentlyAvailable" to 997,
                "restoreRate" to 50
            )
        )
    ))
  }

  @Test
  fun `not registering an adapter, neither at runtime or in the gradle plugin defaults to Any`() {
    val data = GetJsonScalarQuery.Data(
        json = mapOf("1" to "2", "3" to listOf("a", "b"))
    )
    val query = GetJsonScalarQuery()
    val response = query.fromResponse(query.toJson(data))

    assertThat(response.data).isEqualTo(data)
  }
}
