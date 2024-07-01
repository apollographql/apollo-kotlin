package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.toSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class PossibleTypesTest {
  @Test
  fun testPossibleTypes() {
    val schema = File("src/test/sdl/schema.sdl").toSchema()

    val possibleTypes = schema.possibleTypes("Node")

    assertThat(possibleTypes).isEqualTo(setOf("Empire", "Rebellion"))
  }
}
