package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLOperationDefinition
import com.apollographql.apollo3.compiler.frontend.GraphQLParser
import com.apollographql.apollo3.compiler.unified.Edge
import com.apollographql.apollo3.compiler.unified.IrBuilder
import com.apollographql.apollo3.compiler.unified.transitiveReduce
import org.junit.Test
import java.io.File
import java.lang.UnsupportedOperationException

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

    val irOperation = ir.operations.first()
  }

  @Test
  fun testTransitiveReduce() {
    /**
     * 1 -> 2 -> 3;
     * 1 -> 3; // should be removed
     */
    val input = listOf(
        Edge("1", "2"),
        Edge("2", "3"),
        Edge("1", "3"),
    )

    val output = transitiveReduce(input)

    check(
        output == listOf(
            Edge("1", "2"),
            Edge("2", "3"),
        )
    )
  }

  data class Node(val value: String)

  @Test
  fun testTransitiveReduceUsesEquals() {
    /**
     * Same test as [testTransitiveReduce] but using data classes so that the test would fail if referential equality were used
     */
    val input = listOf(
        Edge(Node("1"), Node("2")),
        Edge(Node("2"), Node("3")),
        Edge(Node("1"), Node("3")),
    )

    val output = transitiveReduce(input)

    check(
        output == listOf(
            Edge(Node("1"),Node( "2")),
            Edge(Node("2"),Node( "3")),
        )
    )
  }
}