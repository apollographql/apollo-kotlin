package test.schema

import schema.type.Animal
import schema.type.Cat
import schema.type.Crocodile
import schema.type.Dog
import schema.type.Pet
import schema.schema.__Schema
import kotlin.test.Test
import kotlin.test.assertEquals

class TypesTest {
  @Test
  fun test() {
    assertEquals(Animal.type.name, "Animal")
    assertEquals(Cat.type.name, "Cat")
    assertEquals(
        setOf(Cat.type, Dog.type),
        __Schema.possibleTypes(Pet.type).toSet()
    )
    assertEquals(
        setOf(Cat.type, Dog.type, Crocodile.type),
        __Schema.possibleTypes(Animal.type).toSet()
    )
  }
}
