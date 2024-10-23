package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import com.apollographql.apollo.ast.validateAsSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class PossibleTypesTest {
  @Test
  fun testPossibleTypes() {
    val schema = File("src/test/sdl/schema.sdl").toGQLDocument().validateAsSchema().getOrThrow()

    val possibleTypes = schema.possibleTypes("Node")

    assertThat(possibleTypes).isEqualTo(setOf("Empire", "Rebellion"))
  }
}
