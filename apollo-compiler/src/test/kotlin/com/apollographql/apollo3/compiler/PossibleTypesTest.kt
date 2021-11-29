package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.compiler.introspection.toSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

@OptIn(ApolloExperimental::class)
class PossibleTypesTest {
  @Test
  fun testPossibleTypes() {
    val schema = File("src/test/sdl/schema.sdl").toSchema()

    val possibleTypes = schema.possibleTypes("Node")

    assertThat(possibleTypes).isEqualTo(setOf("Empire", "Rebellion"))
  }
}