package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.frontend.GraphQLParser
import com.apollographql.apollo.compiler.frontend.possibleTypes
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class PossibleTypesTest {
  @Test
  fun testPossibleTypes() {
    val schema = GraphQLParser.parseSchema(File("src/test/sdl/schema.sdl"))

    val possibleTypes = schema.typeDefinition("Node").possibleTypes(schema.typeDefinitions)

    assertThat(possibleTypes).isEqualTo(listOf("Empire", "Rebellion"))
  }
}