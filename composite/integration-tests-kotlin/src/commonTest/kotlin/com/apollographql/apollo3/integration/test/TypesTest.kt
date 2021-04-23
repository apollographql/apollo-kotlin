package com.apollographql.apollo3.integration.test

import com.apollographql.apollo3.integration.normalizer.type.Types
import kotlin.test.Test
import kotlin.test.assertEquals

class TypesTest {
  @Test
  fun test() {
    assertEquals(Types.Character.name, "Character")
    assertEquals(Types.Starship.name, "Starship")
    assertEquals(Types.Droid.implements.toList(), listOf(Types.Character))
    assertEquals(Types.possibleTypes(Types.Character), listOf(Types.Droid, Types.Human))
  }
}