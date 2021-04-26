package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.graphql.ast.GraphQLParser
import com.apollographql.apollo3.graphql.ast.possibleTypes
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class PossibleTypesTest {
  @Test
  fun testPossibleTypes() {
    val schema = GraphQLParser.parseSchema(File("src/test/sdl/schema.sdl"))

    val possibleTypes = schema.typeDefinition("Node").possibleTypes(schema.typeDefinitions)

    assertThat(possibleTypes).isEqualTo(setOf("Empire", "Rebellion"))
  }
}