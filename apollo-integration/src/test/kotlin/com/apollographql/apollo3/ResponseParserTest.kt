package com.apollographql.apollo3

import com.apollographql.apollo3.api.CustomScalarAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.JsonElement
import com.apollographql.apollo3.api.JsonString
import com.apollographql.apollo3.api.parse
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.httpcache.fragment.FilmFragment
import com.apollographql.apollo3.integration.httpcache.fragment.PlanetFragment
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
    val response = EpisodeHeroNameQuery(Input.present(Episode.JEDI)).parse(Utils.readResource("ResponseErrorWithData.json"))
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
    val response = AllFilmsQuery().parse(Utils.readResource("HttpCacheTestAllFilms.json"), ResponseAdapterCache(mapOf(CustomScalars.Date to dateCustomScalarAdapter)))
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
    val response = EpisodeHeroNameQuery(Input.present(Episode.EMPIRE)).parse(
        Buffer().readFrom(javaClass.getResourceAsStream("/ResponseErrorWithData.json")),
        ResponseAdapterCache(emptyMap())
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

  @Test
  fun `forgetting to add a runtime adapter for a scalar registered in the plugin fails`() {
    val data = CharacterDetailsQuery.Data(
        CharacterDetailsQuery.Data.Character.HumanCharacter(
            __typename = "Human",
            name = "Luke",
            birthDate = Date(),
            appearsIn = emptyList(),
            firstAppearsIn = Episode.EMPIRE
        )
    )
    val query = CharacterDetailsQuery(id = "1")
    try {
      query.parse(query.toJson(data))
      error("expected IllegalStateException")
    } catch (e: IllegalArgumentException) {
      assertThat(e.message).contains("Can't map GraphQL type: `Date`")
    }
  }

  @Test
  fun `not registering an adapter, neither at runtime or in the gradle plugin defaults to Any`() {
    val data = GetJsonScalarQuery.Data(
        json = mapOf("1" to "2", "3" to listOf("a", "b"))
    )
    val query = GetJsonScalarQuery()
    val response = query.parse(query.toJson(data))

    assertThat(response.data).isEqualTo(data)
  }
}
