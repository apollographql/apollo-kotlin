package com.apollographql.apollo

import com.apollographql.apollo.api.CustomScalarAdapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.JsonElement
import com.apollographql.apollo.api.JsonString
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.api.toJson
import com.apollographql.apollo.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.httpcache.fragment.FilmFragment
import com.apollographql.apollo.integration.httpcache.fragment.PlanetFragment
import com.apollographql.apollo.integration.httpcache.type.CustomScalars
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
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
  fun allPlanetQuery() {
    val data = AllPlanetsQuery().parse(Utils.readResource("HttpCacheTestAllPlanets.json")).data

    assertThat(data!!.allPlanets?.planets?.size).isEqualTo(60)
    val planets = data.allPlanets?.planets?.mapNotNull {
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
    val firstPlanet = data.allPlanets?.planets?.get(0)
    assertThat((firstPlanet as PlanetFragment).climates).isEqualTo(listOf("arid"))
    assertThat((firstPlanet as PlanetFragment).surfaceWater).isWithin(1.0)
    assertThat(firstPlanet.filmConnection?.totalCount).isEqualTo(5)
    assertThat(firstPlanet.filmConnection?.films?.size).isEqualTo(5)
    assertThat((firstPlanet.filmConnection?.films?.get(0) as FilmFragment).title).isEqualTo("A New Hope")
    assertThat((firstPlanet.filmConnection?.films?.get(0) as FilmFragment).producers).isEqualTo(listOf("Gary Kurtz", "Rick McCallum"))
  }

  @Test
  @Throws(Exception::class)
  fun `errors are properly read`() {
    val response = AllPlanetsQuery().parse(Utils.readResource("ResponseError.json"))
    assertThat(response.hasErrors()).isTrue()
    assertThat(response.errors).containsExactly(
        Error(
            "Cannot query field \"names\" on type \"Species\".",
            listOf(Error.Location(3, 5)),
            emptyMap<String, Any>()
        )
    )
  }

  @Test
  @Throws(Exception::class)
  fun `error with no message, no location and custom attributes`() {
    val response = AllPlanetsQuery().parse(Utils.readResource("ResponseErrorWithNullsAndCustomAttributes.json"))
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
    val response = AllPlanetsQuery().parse(Utils.readResource("ResponseErrorWithCustomAttributes.json"))
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
    val response = EpisodeHeroNameQuery(Input.fromNullable(Episode.JEDI)).parse(Utils.readResource("ResponseErrorWithData.json"))
    val data = response.data
    val errors = response.errors
    assertThat(data).isNotNull()
    assertThat(data!!.hero?.name).isEqualTo("R2-D2")
    assertThat(errors).containsExactly(
        Error(
            "Cannot query field \"names\" on type \"Species\".",
            listOf(Error.Location(3, 5)),
            emptyMap<String, Any>()
        )
    )
  }

  @Test
  @Throws(Exception::class)
  fun allFilmsWithDate() {
    val dateCustomScalarAdapter = object : CustomScalarAdapter<Date> {
      private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      override fun decode(jsonElement: JsonElement) = DATE_FORMAT.parse(jsonElement.toRawValue().toString())
      override fun encode(value: Date) = JsonString(DATE_FORMAT.format(value))
    }
    val response = AllFilmsQuery().parse(Utils.readResource("HttpCacheTestAllFilms.json"), CustomScalarAdapters(mapOf(CustomScalars.Date to dateCustomScalarAdapter)))
    assertThat(response.hasErrors()).isFalse()
    assertThat(response.data!!.allFilms?.films).hasSize(6)
    assertThat(response.data!!.allFilms?.films?.map { dateCustomScalarAdapter.encode(it!!.releaseDate).value }).isEqualTo(
        listOf("1977-05-25", "1980-05-17", "1983-05-25", "1999-05-19", "2002-05-16", "2005-05-19"))
  }

  @Test
  @Throws(Exception::class)
  fun dataNull() {
    val response = HeroNameQuery().parse(Utils.readResource("ResponseDataNull.json"))
    assertThat(response.data).isNull()
    assertThat(response.hasErrors()).isFalse()
  }

  @Test
  @Throws(Exception::class)
  fun fieldMissing() {
    try {
      HeroNameQuery().parse(Utils.readResource("ResponseDataMissing.json"))
      error("an error was expected")
    } catch (e: NullPointerException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun operationResponseParser() {
    val data = HeroNameQuery().parse(Utils.readResource("HeroNameResponse.json")).data
    assertThat(data!!.hero?.name).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun operationJsonWriter() {
    val expected = Utils.readResource("OperationJsonWriter.json")
    val query = AllPlanetsQuery()
    val data = query.parse(expected).data
    val actual = query.toJson(data!!, "  ")
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  @Throws(Exception::class)
  fun parseSuccessOperationRawResponse() {
    val query = AllPlanetsQuery()
    val response = query.parse(Utils.readResource("AllPlanetsNullableField.json"))
    assertThat(response.operation).isEqualTo(query)
    assertThat(response.hasErrors()).isFalse()
    assertThat(response.data).isNotNull()
    assertThat(response.data!!.allPlanets?.planets).isNotEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun parseErrorOperationRawResponse() {
    val response = EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)).parse(
        Buffer().readFrom(javaClass.getResourceAsStream("/ResponseErrorWithData.json")),
        CustomScalarAdapters(emptyMap())
    )
    val data = response.data
    val errors = response.errors

    assertThat(data).isNotNull()
    assertThat(data!!.hero).isNotNull()
    assertThat(data.hero?.name).isEqualTo("R2-D2")
    assertThat(errors).containsExactly(
        Error(
            "Cannot query field \"names\" on type \"Species\".",
            listOf(Error.Location(3, 5)),
            emptyMap<String, Any>()
        )
    )
  }

  @Test
  @Throws(Exception::class)
  fun `extensions are read from response`() {
    val query = HeroNameQuery()
    val extensions = query.parse(Utils.readResource("HeroNameResponse.json")).extensions
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
}
