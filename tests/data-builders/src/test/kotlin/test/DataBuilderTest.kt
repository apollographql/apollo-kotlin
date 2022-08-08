package test

import MyLong
import data.builders.GetAliasesQuery
import data.builders.GetAnimalQuery
import data.builders.GetCustomScalarQuery
import data.builders.GetDirectionQuery
import data.builders.GetFelineQuery
import data.builders.GetIntQuery
import data.builders.PutIntMutation
import data.builders.type.Direction
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
    }

    assertEquals(42, data.long1?.value)
    assertEquals(43, data.long2?.value)
    assertEquals(44, data.long3)
  }
}
