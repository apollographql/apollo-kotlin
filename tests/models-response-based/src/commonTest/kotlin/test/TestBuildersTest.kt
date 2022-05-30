package test

import codegen.models.AllPlanetsQuery
import codegen.models.BirthdateQuery
import codegen.models.EpisodeQuery
import codegen.models.HeroAndFriendsWithTypenameQuery
import codegen.models.MergedFieldWithSameShapeQuery
import codegen.models.StarshipQuery
import codegen.models.test.AllPlanetsQuery_TestBuilder.Data
import codegen.models.test.BirthdateQuery_TestBuilder.Data
import codegen.models.test.EpisodeQuery_TestBuilder.Data
import codegen.models.test.HeroAndFriendsWithTypenameQuery_TestBuilder.Data
import codegen.models.test.MergedFieldWithSameShapeQuery_TestBuilder.Data
import codegen.models.test.StarshipQuery_TestBuilder.Data
import codegen.models.type.Date
import codegen.models.type.Episode
import codegen.models.type.StarshipType
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledListType
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledNotNullType
import com.apollographql.apollo3.api.CompiledType
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.test.DefaultTestResolver
import com.apollographql.apollo3.api.test.TestResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
      assertEquals("Hero: __typename is not known at compile-time for this type. Please specify it explicitly (allowed values: Human, Droid)", e.message)
    }

    try {
      HeroAndFriendsWithTypenameQuery.Data {
        hero = hero {
          name = "R2-D2"
          __typename = "Droid"
        }
      }
      fail("An exception was expected")
    } catch (e: IllegalStateException) {
      assertEquals("Friend: __typename is not known at compile-time for this type. Please specify it explicitly (allowed values: Human, Droid)", e.message)
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
    val data = BirthdateQuery.Data(customScalarAdapters = CustomScalarAdapters.Builder().add(Date.type, adapter).build()) {
      hero = hero {
        birthDate = "12345"
      }
    }

    assertEquals(12345L, data.hero?.birthDate)
  }

  @Test
  fun customDefaultTestResolver() {
    val defaultString = "default"
    val defaultInt = 5
    val defaultFloat = 7.0
    val myTestResolver = object : DefaultTestResolver() {
      override fun resolveListSize(path: List<Any>): Int {
        return 1
      }

      override fun resolveInt(path: List<Any>): Int {
        return defaultInt
      }

      override fun resolveString(path: List<Any>): String {
        return defaultString
      }

      override fun resolveFloat(path: List<Any>): Double {
        return defaultFloat
      }

      override fun resolveComposite(path: List<Any>, ctors: Array<out () -> Map<String, Any?>>): Map<String, Any?> {
        return ctors[0]()
      }
    }

    val data = AllPlanetsQuery.Data(testResolver = myTestResolver) {}

    val expected = AllPlanetsQuery.Data(
        allPlanets = AllPlanetsQuery.Data.AllPlanets(
            planets = listOf(
                AllPlanetsQuery.Data.AllPlanets.Planet(
                    __typename = "Planet",
                    name = defaultString,
                    climates = listOf(defaultString),
                    surfaceWater = defaultFloat,
                    filmConnection = AllPlanetsQuery.Data.AllPlanets.Planet.FilmConnection(
                        totalCount = defaultInt,
                        films = listOf(
                            AllPlanetsQuery.Data.AllPlanets.Planet.FilmConnection.Film(
                                __typename = "Film",
                                title = defaultString,
                                producers = listOf(defaultString)
                            )
                        )
                    )
                )
            )
        )
    )

    assertEquals(expected, data)
  }

  @Test
  fun customTestResolver() {
    val defaultString = "default"
    val defaultInt = 5
    val defaultFloat = 7.0

    val myTestResolver = object : TestResolver {
      override fun <T> resolve(
          responseName: String,
          compiledType: CompiledType,
          enumValues: List<String>,
          ctors: Array<out () -> Map<String, Any?>>?,
      ): T {
        return when (compiledType) {
          is CompiledNotNullType -> resolve(responseName, compiledType.ofType, enumValues, ctors)
          is CompiledListType -> listOf(resolve<Any>(responseName, compiledType.ofType, enumValues, ctors))
          is CompiledNamedType -> {
            when (compiledType.name) {
              "Int" -> defaultInt
              "Float" -> defaultFloat
              "String" -> defaultString
              else -> ctors!![0]()
            }
          }
        } as T

      }
    }
    val data = AllPlanetsQuery.Data(testResolver = myTestResolver) {}

    val expected = AllPlanetsQuery.Data(
        allPlanets = AllPlanetsQuery.Data.AllPlanets(
            planets = listOf(
                AllPlanetsQuery.Data.AllPlanets.Planet(
                    __typename = "Planet",
                    name = defaultString,
                    climates = listOf(defaultString),
                    surfaceWater = defaultFloat,
                    filmConnection = AllPlanetsQuery.Data.AllPlanets.Planet.FilmConnection(
                        totalCount = defaultInt,
                        films = listOf(
                            AllPlanetsQuery.Data.AllPlanets.Planet.FilmConnection.Film(
                                __typename = "Film",
                                title = defaultString,
                                producers = listOf(defaultString)
                            )
                        )
                    )
                )
            )
        )
    )

    assertEquals(expected, data)
  }

  @Test
  fun enum() {
    val data = EpisodeQuery.Data {
      hero = hero {
        appearsIn = listOf(Episode.JEDI.rawValue)
      }
    }

    assertEquals(Episode.JEDI, data.hero?.appearsIn?.single())
  }

  @Test
  fun enumResolve() {
    val data = EpisodeQuery.Data {
    }

    val enums = data.hero?.appearsIn
    assertIs<List<*>>(enums)
    enums.forEach {
      assertIs<Episode>(it)
    }
  }

  @Test
  fun enumAsSealedClass() {
    val data = StarshipQuery.Data {
      starship = starship {
        starshipType = StarshipType.STAR_CRUISER.rawValue
      }
    }
    assertEquals(StarshipType.STAR_CRUISER, data.starship?.starshipType)
  }

  @Test
  fun enumAsSealedClassResolve() {
    val data = StarshipQuery.Data {
    }

    val sealedClass = data.starship?.starshipType
    assertIs<StarshipType>(sealedClass)
  }


  @Test
  fun errorIfForgotToAssignField() {
    val e1 = assertFailsWith<IllegalStateException>() {
      AllPlanetsQuery.Data {
        /* allPlanets = */ allPlanets {
          planets = listOf(
              planet {
                name = "Tatoine"
              }
          )
        }
      }
    }
    assertEquals("Builder function was called but its result was not assigned to the corresponding field `allPlanets` which is certainly a mistake", e1.message)

    val e2 = assertFailsWith<IllegalStateException>() {
      AllPlanetsQuery.Data {
        allPlanets = allPlanets {
          /* planets = listOf( */
          planet {
            name = "Tatoine"
          }
          /* ) */
        }
      }
    }
    assertEquals("Builder function was called but its result was not assigned to the corresponding field `planets` which is certainly a mistake", e2.message)
  }
}
