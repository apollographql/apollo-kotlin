package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.graphql.ast.GQLDescribed
import com.apollographql.apollo3.graphql.ast.GQLTypeDefinition
import com.apollographql.apollo3.graphql.ast.GraphQLParser
import com.apollographql.apollo3.graphql.ast.mergeTypeExtensions
import com.apollographql.apollo3.graphql.ast.toSchema
import com.apollographql.apollo3.graphql.ast.usedTypeNames
import com.apollographql.apollo3.graphql.ast.validateAsSchema
import com.apollographql.apollo3.graphql.ast.withBuiltinDefinitions
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class UsedTypesTest(val name: String, val graphQLFile: File) {

  @Test
  fun testTypes() {
    val (schema, executableDefinitions) = GraphQLParser.parseDocument(graphQLFile)
        .flatMap {
          val (typeDefinitions, operation) = it.definitions.partition {
            it is GQLTypeDefinition
          }
          it.copy(definitions = typeDefinitions).mergeTypeExtensions().mapValue {
            it to operation
          }
        }
        .appendIssues {
          it.first.validateAsSchema()
        }
        .mapValue {
          it.first.withBuiltinDefinitions().toSchema() to it.second
        }
        .orThrow()

    val usedTypes = executableDefinitions
        .usedTypeNames(schema)

    Truth.assertThat(usedTypes)
        .isEqualTo(
            executableDefinitions
                .filterIsInstance<GQLDescribed>()
                .first()
                .description
                ?.split("\n")?.map {
                  it.trim()
                }?.toSet()
        )
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      return File("src/test/usedtypes/")
          .listFiles()!!
          .map { arrayOf(it.nameWithoutExtension, it) }
    }
  }
}
