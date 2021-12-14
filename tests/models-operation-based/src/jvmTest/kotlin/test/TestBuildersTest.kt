package test

import codegen.models.AllPlanetsQuery
import codegen.models.HeroAndFriendsWithTypenameQuery
import codegen.models.MergedFieldWithSameShapeQuery
import codegen.models.test.AllPlanetsQuery_TestBuilder.Data
import codegen.models.test.HeroAndFriendsWithTypenameQuery_TestBuilder.Data
import codegen.models.test.MergedFieldWithSameShapeQuery_TestBuilder.Data
import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

@OptIn(ApolloExperimental::class)
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

    assertEquals("Tatoine", data.allPlanets?.planets?.get(0)?.planetFragment?.name)
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
    assertEquals("Earth", hero?.onHuman?.property)
  }
}
