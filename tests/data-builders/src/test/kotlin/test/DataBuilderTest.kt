package test

import MyLong
import com.apollographql.apollo3.api.FakeResolver
import com.apollographql.apollo3.api.FakeResolverContext
import data.builders.GetAliasesQuery
import data.builders.GetAnimalQuery
import data.builders.GetCustomScalarQuery
import data.builders.GetDirectionQuery
import data.builders.GetEverythingQuery
import data.builders.GetFelineQuery
import data.builders.GetIntQuery
import data.builders.GetPartialQuery
import data.builders.PutIntMutation
import data.builders.type.Direction
import data.builders.type.Lion
import data.builders.type.buildCat
import data.builders.type.buildLion
import kotlin.test.Test
import kotlin.test.assertEquals

class DataBuilderTest {
  @Test
  fun nullabilityTest() {
    val data = GetIntQuery.Data {
      nullableInt = null
      nonNullableInt = 42
    }

    assertEquals(null, data.nullableInt)
    assertEquals(42, data.nonNullableInt)
  }

  @Test
  fun aliasTest() {
    val data = GetAliasesQuery.Data {
      this["aliasedNullableInt"] = 50
      cat = buildCat {
        species = "Cat"
      }
      this["aliasedCat"] = buildCat {
        species = "AliasedCat"
      }
    }

    assertEquals(50, data.aliasedNullableInt)
    assertEquals("Cat", data.cat.species)
    assertEquals("AliasedCat", data.aliasedCat.species)
  }

  @Test
  fun mutationTest() {
    val data = PutIntMutation.Data {
      nullableInt = null
    }

    assertEquals(null, data.nullableInt)
  }

  @Test
  fun interfaceTest() {
    val data = GetAnimalQuery.Data {
      animal = buildLion {
        species = "LionSpecies"
        roar = "Rooooaaarr"
      }
    }

    assertEquals("Lion", data.animal.__typename)
    assertEquals("LionSpecies", data.animal.species)
    assertEquals("Rooooaaarr", data.animal.onLion?.roar)
  }

  @Test
  fun unionTest1() {
    val data = GetFelineQuery.Data {
      feline = buildLion {
        species = "LionSpecies"
        roar = "Rooooaaarr"
      }
    }

    assertEquals("Lion", data.feline.__typename)
    assertEquals(null, data.feline.onCat)
  }

  @Test
  fun unionTest2() {
    val data = GetFelineQuery.Data {
      feline = buildCat {
        species = "CatSpecies"
        mustaches = 5
      }
    }

    assertEquals("Cat", data.feline.__typename)
    assertEquals(5, data.feline.onCat?.mustaches)
  }

  @Test
  fun enumTest() {
    val data = GetDirectionQuery.Data {
      direction = Direction.NORTH
    }

    assertEquals(Direction.NORTH, data.direction)
  }

  @Test
  fun customScalarTest() {
    val data = GetCustomScalarQuery.Data {
      long1 = MyLong(42)
      long2 = MyLong(43)
      long3 = 44
      listOfListOfLong1 = listOf(listOf(MyLong(42)))
    }

    assertEquals(42, data.long1?.value)
    assertEquals(43, data.long2?.value)
    assertEquals(44, data.long3)
  }

  @Test
  fun fakeValues() {
    val data = GetEverythingQuery.Data()

    assertEquals(Direction.SOUTH, data.direction)
    assertEquals(0, data.nullableInt)
    assertEquals(1, data.nonNullableInt)
    assertEquals(listOf(
        listOf(2, 3, 4),
        listOf(5, 6, 7),
        listOf(8, 9, 10)
    ), data.listOfListOfInt)
    assertEquals(11, data.cat.mustaches)
    assertEquals("Lion", data.animal.__typename)
    assertEquals("Cat", data.feline.__typename)

    println(data)
  }

  @Test
  fun partialFakeValues() {
    val data = GetPartialQuery.Data {
      listOfListOfAnimal = listOf(
          listOf(
              buildLion {
                species = "FooSpecies"
              }
          )
      )
    }

    assertEquals(
        GetPartialQuery.Data(
            listOfListOfAnimal = listOf(
                listOf(
                    GetPartialQuery.ListOfListOfAnimal(
                        __typename = "Lion",
                        species = "FooSpecies",
                        onLion = GetPartialQuery.OnLion("roar")
                    )
                )
            ),
        ),
        data
    )
  }

  class MyFakeResolver : FakeResolver {
    override fun resolveLeaf(context: FakeResolverContext): Any {
      return when (context.mergedField.type.leafType().name) {
        "Long1" -> "45" // build-time => this needs to be resolved to Json
        "Long2" -> MyLong(46) // run-time
        "Long3" -> 47L // mapped to Any
        else -> error("")
      }
    }

    override fun resolveListSize(context: FakeResolverContext): Int {
      return 1
    }

    override fun resolveMaybeNull(context: FakeResolverContext): Boolean {
      return false
    }

    override fun resolveTypename(context: FakeResolverContext): String {
      TODO("Not yet implemented")
    }
  }

  @Test
  fun customScalarFakeValues() {
    val data = GetCustomScalarQuery.Data(MyFakeResolver())

    assertEquals(45L, data.long1?.value)
    assertEquals(46L, data.long2?.value)
    assertEquals(47, data.long3) // AnyAdapter will try to fit the smallest possible number
  }
}
