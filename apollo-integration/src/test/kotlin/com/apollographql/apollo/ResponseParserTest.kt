package com.apollographql.apollo

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.httpcache.fragment.FilmFragment
import com.apollographql.apollo.integration.httpcache.fragment.PlanetFragment
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ResponseParserTest {

  @Test
  @Throws(Exception::class)
  fun allPlanetQuery() {
    val data = AllPlanetsQuery().parse(Utils.resourceText("HttpCacheTestAllPlanets.json")).data

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
}