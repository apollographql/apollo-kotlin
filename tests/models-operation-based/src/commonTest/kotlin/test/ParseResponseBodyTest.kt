package test

import codegen.models.AllPlanetsQuery
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.api.parseJsonResponse
import okio.Buffer
import testFixtureToJsonReader
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseResponseBodyTest {
  /**
   * Nothing really specific here, it's just a bigger response
   */
  @Test
  fun allPlanetQuery() {
    val data = AllPlanetsQuery().parseJsonResponse(testFixtureToJsonReader("AllPlanets.json")).data

    assertEquals(data!!.allPlanets?.planets?.size, 60)
    val planets = data.allPlanets?.planets?.mapNotNull {
      it?.planetFragment?.name
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
    assertEquals(firstPlanet?.planetFragment?.climates, listOf("arid"))
    assertEquals(firstPlanet?.planetFragment?.surfaceWater, 1.0)
    assertEquals(firstPlanet?.filmConnection?.totalCount, 5)
    assertEquals(firstPlanet?.filmConnection?.films?.size, 5)
    assertEquals(firstPlanet?.filmConnection?.films?.get(0)?.filmFragment?.title, "A New Hope")
    assertEquals(firstPlanet?.filmConnection?.films?.get(0)?.filmFragment?.producers, listOf("Gary Kurtz", "Rick McCallum"))
  }

  @Test
  @Throws(Exception::class)
  fun operationJsonWriter() {
    val expected = testFixtureToUtf8("OperationJsonWriter.json")

    val query = AllPlanetsQuery()
    val data = query.parseJsonResponse(Buffer().writeUtf8(expected).jsonReader()).data
    val actual = buildJsonString(indent = "  ") {
      query.composeJsonResponse(this, data!!)
    }

    /**
     * operationBased models do not respect the order of fields
     * when fragments are involved so just check for Map equivalence
     *
     * If this fails, you can update "OperationJsonWriter.json" in models-response-based
     */
    val expectedMap = Buffer().writeUtf8(expected).jsonReader().readAny()
    val actualMap = Buffer().writeUtf8(actual).jsonReader().readAny()

    assertEquals(expectedMap, actualMap)
  }
}
