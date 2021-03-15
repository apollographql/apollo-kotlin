package com.apollographql.apollo3.interfaces

import com.apollographql.apollo3.Utils
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.httpcache.fragment.FilmFragment
import com.apollographql.apollo3.integration.httpcache.fragment.PlanetFragment
import com.apollographql.apollo3.integration.normalizer.CharacterDetailsQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import org.junit.Test
import java.util.Date

class ResponseParserTest {
  @Test
  @Throws(Exception::class)
  fun allPlanetQuery() {
    val data = AllPlanetsQuery().fromResponse(Utils.readResource("HttpCacheTestAllPlanets.json")).data

    Truth.assertThat(data!!.allPlanets?.planets?.size).isEqualTo(60)
    val planets = data.allPlanets?.planets?.mapNotNull {
      (it as PlanetFragment).name
    }
    Truth.assertThat(planets).isEqualTo(("Tatooine, Alderaan, Yavin IV, Hoth, Dagobah, Bespin, Endor, Naboo, "
        + "Coruscant, Kamino, Geonosis, Utapau, Mustafar, Kashyyyk, Polis Massa, Mygeeto, Felucia, Cato Neimoidia, "
        + "Saleucami, Stewjon, Eriadu, Corellia, Rodia, Nal Hutta, Dantooine, Bestine IV, Ord Mantell, unknown, "
        + "Trandosha, Socorro, Mon Cala, Chandrila, Sullust, Toydaria, Malastare, Dathomir, Ryloth, Aleen Minor, "
        + "Vulpter, Troiken, Tund, Haruun Kal, Cerea, Glee Anselm, Iridonia, Tholoth, Iktotch, Quermia, Dorin, "
        + "Champala, Mirial, Serenno, Concord Dawn, Zolan, Ojom, Skako, Muunilinst, Shili, Kalee, Umbara")
        .split(",")
        .map { it.trim() }
    )
    val firstPlanet = data.allPlanets?.planets?.get(0)
    Truth.assertThat((firstPlanet as PlanetFragment).climates).isEqualTo(listOf("arid"))
    Truth.assertThat((firstPlanet as PlanetFragment).surfaceWater).isWithin(1.0)
    Truth.assertThat(firstPlanet.filmConnection?.totalCount).isEqualTo(5)
    Truth.assertThat(firstPlanet.filmConnection?.films?.size).isEqualTo(5)
    Truth.assertThat((firstPlanet.filmConnection?.films?.get(0) as FilmFragment).title).isEqualTo("A New Hope")
    Truth.assertThat((firstPlanet.filmConnection?.films?.get(0) as FilmFragment).producers).isEqualTo(listOf("Gary Kurtz", "Rick McCallum"))
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
      query.fromResponse(query.toJson(data))
      error("expected IllegalStateException")
    } catch (e: IllegalStateException) {
      Truth.assertThat(e.message).contains("Can't map GraphQL type: `Date`")
    }
  }
}