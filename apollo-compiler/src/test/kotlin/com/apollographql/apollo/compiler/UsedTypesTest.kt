package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.gql.GQLDescribed
import com.apollographql.apollo.compiler.parser.gql.GQLOperationDefinition
import com.apollographql.apollo.compiler.parser.gql.GQLTypeDefinition
import com.apollographql.apollo.compiler.parser.gql.GraphQLParser
import com.apollographql.apollo.compiler.parser.gql.mergeTypeExtensions
import com.apollographql.apollo.compiler.parser.gql.toSchema
import com.apollographql.apollo.compiler.parser.gql.usedTypeNames
import com.apollographql.apollo.compiler.parser.gql.validateAsSchema
import com.apollographql.apollo.compiler.parser.gql.withBuiltinTypes
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
          it.first.withBuiltinTypes().toSchema() to it.second
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
          .listFiles()
          .map { arrayOf(it.nameWithoutExtension, it) }
    }
  }
}
