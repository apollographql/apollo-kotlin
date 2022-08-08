package test

import data.builders.GetAliasesQuery
import data.builders.GetAnimalQuery
import data.builders.GetIntQuery
import data.builders.PutIntMutation
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
    assertEquals("Cat", data.cat?.species)
    assertEquals("AliasedCat", data.aliasedCat?.species)
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

    assertEquals("Lion", data.animal?.__typename)
    assertEquals("LionSpecies", data.animal?.species)
    assertEquals("Rooooaaarr", data.animal?.onLion?.roar)
  }
}
