package test

import com.apollographql.apollo.api.Builder
import com.apollographql.apollo.api.DefaultFakeResolver
import com.apollographql.apollo.api.FakeResolverContext
import com.apollographql.apollo.api.Optional
import com.example.MyLong
import data.builders.GetAliasesQuery
import data.builders.GetAnimalQuery
import data.builders.GetCatAnimalQuery
import data.builders.GetCustomScalarQuery
import data.builders.GetDirectionQuery
import data.builders.GetEgotisticalCatQuery
import data.builders.GetEverythingQuery
import data.builders.GetFelineQuery
import data.builders.GetIntQuery
import data.builders.GetNodeQuery
import data.builders.GetPartialQuery
import data.builders.GetProductQuery
import data.builders.PutIntMutation
import data.builders.SkipQuery
import data.builders.schema.__Schema
import data.builders.type.CatBuilder
import data.builders.type.Direction
import data.builders.type.__CustomScalarAdapters
import data.builders.type.buildCat
import data.builders.type.buildLion
import data.builders.type.buildOtherAnimal
import data.builders.type.buildOtherFeline
import data.builders.type.buildProduct
import data.builders.type.buildPromo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
  fun otherInterfaceImplementationTest() {
    val data = GetAnimalQuery.Data {
      animal = buildOtherAnimal("Gazelle") {
        species = "GazelleSpecies"
      }
    }

    assertEquals("Gazelle", data.animal.__typename)
    assertEquals("GazelleSpecies", data.animal.species)
    assertNotNull(data.animal.id)
    assertNull(data.animal.onLion)
  }

  @Test
  fun unusedTypesAreNotGenerated() {
    try {
      Class.forName("data.builders.type.UnusedTypeBuilder")
      error("The data.builders.type.UnusedTypeBuilder class should not exist")
    } catch (_: ClassNotFoundException) {

    }
  }

  @Test
  fun fieldsOnInterfacesAreGeneratedInObjectBuilders() {
    val data = GetNodeQuery.Data {
      node = buildCat {
        id = "42"
      }
    }

    assertEquals("42", data.node?.id)
  }

  @Test
  fun unusedFieldsAreNotGenerated() {
    val methods = CatBuilder::class.java.declaredMethods

    check(methods.any { it.name == "setMustaches" })
    check(methods.none { it.name == "setUnusedField" })
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
  fun otherUnionMemberTest() {
    val data = GetFelineQuery.Data {
      feline = buildOtherFeline("Tiger") {
      }
    }

    assertEquals("Tiger", data.feline.__typename)
    assertEquals(null, data.feline.onCat)
    assertEquals(null, data.feline.onAnimal)
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

    assertEquals(Direction.NORTH, data.direction)
    assertEquals(-34, data.nullableInt)
    assertEquals(-99, data.nonNullableInt)
    assertEquals(listOf(
        listOf(73, 74, 75),
        listOf(4, 5, 6),
        listOf(35, 36, 37)
    ), data.listOfListOfInt)
    assertEquals(53, data.cat.mustaches)
    assertEquals("Cat", data.animal.__typename)
    assertEquals("Lion", data.feline.__typename)
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
                        id = "574122978",
                        species = "FooSpecies",
                        onLion = GetPartialQuery.OnLion("roar")
                    )
                )
            ),
        ),
        data
    )
  }

  class MyFakeResolver : DefaultFakeResolver(__Schema.all) {
    override fun resolveLeaf(context: FakeResolverContext): Any {
      return when (context.mergedField.type.rawType().name) {
        "Long1" -> MyLong(45) // build-time
        "Long2" -> MyLong(46) // run-time
        "Long3" -> 47L // mapped to Any
        else -> super.resolveLeaf(context)
      }
    }
  }

  @Test
  fun customScalarFakeValues() {
    val data = GetCustomScalarQuery.Data(MyFakeResolver())

    assertEquals(45L, data.long1?.value)
    assertEquals(46L, data.long2?.value)
    assertEquals(47, data.long3) // AnyDataAdapter will try to fit the smallest possible number
  }

  @Test
  fun fakeValuesCanBeReused() {
    val cat = Builder(__CustomScalarAdapters).buildCat {
      id = "42"
      bestFriend = buildCat {
        id = "42"
      }
    }
    val resolver = MyFakeResolver()

    val data = GetEgotisticalCatQuery.Data(resolver) {
      this.cat = cat
    }
    val data2 = GetCatAnimalQuery.Data(resolver) {
      this.animal = cat
    }

    val cat1 = data.cat
    val cat2 = data.cat.bestFriend
    val cat3 = data2.animal

    assertEquals(cat1.species, cat2.species)
    assertEquals(cat1.mustaches, cat2.onCat?.mustaches)
    assertEquals(cat1.species, cat3.onCat?.species)
    assertEquals(cat1.mustaches, cat3.onCat?.mustaches)
  }

  @Test
  fun using__stableId() {
    val productData = Builder(__CustomScalarAdapters).buildProduct {
      this["__stableId"] = "42"
    }

    val data = GetProductQuery.Data {
      product = productData
      promo = buildPromo {
        product = productData
      }
    }

    assertEquals(data.product?.name, data.promo?.product?.name)
    assertEquals(data.product?.price, data.promo?.product?.price)
  }

  @Test
  fun skip() {
    val data = SkipQuery.Data {
      this["nonNullableInt"] = Optional.Absent
    }

    assertNull(data.nonNullableInt)
  }
}
