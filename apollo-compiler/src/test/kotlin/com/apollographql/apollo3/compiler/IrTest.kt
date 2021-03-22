package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLOperationDefinition
import com.apollographql.apollo3.compiler.frontend.GraphQLParser
import com.apollographql.apollo3.compiler.unified.IrBuilder
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeSpec
import com.squareup.kotlinpoet.FileSpec
import org.junit.Test
import java.io.File

class IrTest {
  @Test
  fun test() {
    val schema = GraphQLParser.parseSchema(
        File("src/test/graphql/com/example/inline_frgament_intersection/schema.sdl")
    )
    val operation = GraphQLParser.parseOperations(
        File("src/test/graphql/com/example/inline_frgament_intersection/TestOperation.graphql"),
        schema
    )


    val ir = IrBuilder(
        schema = schema,
        operationDefinitions = operation.orThrow().definitions.filterIsInstance<GQLOperationDefinition>(),
        metadataFragmentDefinitions = emptyList(),
        fragmentDefinitions = operation.orThrow().definitions.filterIsInstance<GQLFragmentDefinition>(),
        alwaysGenerateTypesMatching = emptySet(),
        customScalarToKotlinName = emptyMap(),
        packageNameProvider = DefaultPackageNameProvider("com.example", Roots(emptySet()), "com.example")
    ).build()

    if (ir.operations.isEmpty()) {
      throw UnsupportedOperationException("No operation found")
    }
    if (ir.operations.size > 1) {
      throw UnsupportedOperationException("Multiple operations are not supported")
    }

    val irOperation = ir.operations.first()
    FileSpec.builder("com.example", irOperation.name)
        .apply {
          irOperation.dataField.fieldSets.forEach {
            addType(it.typeSpec())
          }
        }
        .build()
        .writeTo(File("build/irTest"))
  }
}