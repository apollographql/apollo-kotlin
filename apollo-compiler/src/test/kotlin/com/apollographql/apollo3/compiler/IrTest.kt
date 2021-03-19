package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLOperationDefinition
import com.apollographql.apollo3.compiler.frontend.GraphQLParser
import com.apollographql.apollo3.compiler.unified.IrBuilder
import org.junit.Test
import java.io.File

class IrTest {
  @Test
  fun test() {
    val schema = GraphQLParser.parseSchema(
        File("src/test/graphql/schema.sdl")
    )
    val operation = GraphQLParser.parseOperations(
        File("src/test/graphql/com/example/hero_name/TestOperation.graphql"),
        schema
    )

    val ir = IrBuilder(
        schema = schema,
        operationDefinitions = operation.orThrow().definitions.filterIsInstance<GQLOperationDefinition>(),
        metadataFragmentDefinitions = emptyList(),
        fragmentDefinitions = operation.orThrow().definitions.filterIsInstance<GQLFragmentDefinition>(),
        alwaysGenerateTypesMatching = emptySet()
    ).build()

    if (ir.operations.isEmpty()) {
      throw UnsupportedOperationException("No operation found")
    }
    if (ir.operations.size > 1) {
      throw UnsupportedOperationException("Multiple operations are not supported")
    }

    println(ir.operations.first())
  }
}