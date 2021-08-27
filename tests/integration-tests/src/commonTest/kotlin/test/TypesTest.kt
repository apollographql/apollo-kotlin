package test

import com.apollographql.apollo3.integration.normalizer.type.Character
import com.apollographql.apollo3.integration.normalizer.type.Starship
import kotlin.test.Test
import kotlin.test.assertEquals

class TypesTest {
  @Test
  fun test() {
    assertEquals(Character.type.name, "Character")
    assertEquals(Starship.type.name, "Starship")
  }
}