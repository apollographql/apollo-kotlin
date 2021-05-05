package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.graphql.ast.possibleTypes
import com.apollographql.apollo3.graphql.ast.toGraphQLSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class PossibleTypesTest {
  @Test
  fun testPossibleTypes() {
    val schema = File("src/test/sdl/schema.sdl").toGraphQLSchema()

    val possibleTypes = schema.typeDefinition("Node").possibleTypes(schema.typeDefinitions)

    assertThat(possibleTypes).isEqualTo(setOf("Empire", "Rebellion"))
  }
}