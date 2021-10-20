package test

import codegen.models.AllPlanetsQuery
import codegen.models.BirthdateQuery
import codegen.models.HeroAndFriendsWithTypenameQuery
import codegen.models.MergedFieldWithSameShapeQuery
import codegen.models.test.AllPlanetsQuery_TestBuilder.Data
import codegen.models.test.BirthdateQuery_TestBuilder.Data
import codegen.models.test.HeroAndFriendsWithTypenameQuery_TestBuilder.Data
import codegen.models.test.MergedFieldWithSameShapeQuery_TestBuilder.Data
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.fail

class TestBuildersTest {
  @Test
  fun allPlanets() {
    val data = AllPlanetsQuery.Data {
      allPlanets = allPlanets {
        planets = listOf(
            planet {
              name = "Tatoine"
            }
        )
      }
    }

    assertEquals("Tatoine", data.allPlanets?.planets?.get(0)?.name)
  }

  @Test
  fun nullIsWorking() {
    val data = AllPlanetsQuery.Data {
      allPlanets = null
    }

    assertNull(data.allPlanets)
  }

  @Test
  fun typenameMustBeSet() {
    try {
      HeroAndFriendsWithTypenameQuery.Data {}
      fail("An exception was expected")
    } catch (e: IllegalStateException) {
      assertEquals("__typename is not known at compile-time for this type. Please specify it explicitely", e.message)
    }
  }

  @Test
  fun polymorphic() {
    val data = MergedFieldWithSameShapeQuery.Data {
      hero = humanHero {
        property = "Earth"
      }
    }

    val hero = data.hero
    assertIs<MergedFieldWithSameShapeQuery.Data.HumanHero>(hero)
    assertEquals("Earth", hero.property)
  }

  @Test
  fun customScalar() {
    /**
     * A very simple adapter that simply converts to Long in order to avoid pulling kotlinx.datetime in the classpath
     */
    val adapter = object : Adapter<Long> {
      override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Long {
        return reader.nextString()!!.toLong()
      }

      override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Long) {
        TODO("Not yet implemented")
      }

    }
    val data = BirthdateQuery.Data(customScalarAdapters = CustomScalarAdapters(mapOf("Date" to adapter))) {
      hero = hero {
        birthDate = "12345"
      }
    }

    assertEquals(12345L, data.hero?.birthDate)
  }
}