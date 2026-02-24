package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.DirectiveRedefinition
import com.apollographql.apollo.ast.GraphQLIssue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.validateAsExecutable
import com.apollographql.apollo.ast.validateAsSchema
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class ExecutableValidationTest {
  @Test
  fun testValidation(@TestParameter(valuesProvider = ParametersProvider::class) graphQLFile: File) {
    findSchemaAndCheck(graphQLFile) { schema ->
      val parserOptions = graphQLFile.pragmas().toParserOptions()
      val parseResult = graphQLFile.source().buffer().parseAsGQLDocument(graphQLFile.name, parserOptions)
      val issues = if (parseResult.issues.isNotEmpty()) {
        parseResult.issues
      } else {
        parseResult.getOrThrow().validateAsExecutable(schema = schema).issues
      }

      issues.serialize()
    }
  }

  private fun findSchemaAndCheck(graphQLFile: File, block: (schema: Schema) -> String) {
    var parent = graphQLFile.parentFile

    // We're in src/test/validation/operation/...
    // Find the closest schema
    var schema: Schema? = null
    while (parent.name != "test") {
      val candidate = parent.resolve("schema.graphqls")
      if (candidate.exists()) {
        val pragmas = candidate.pragmas()
        schema = candidate.toGQLDocument().validateAsSchema(
            candidate.pragmas().toSchemaValidationOptions()
        ).let {
          it.issues.forEach {
            when (it) {
              is DirectiveRedefinition -> if (Pragma.allowDirectiveRedefinition in pragmas) return@forEach
              is GraphQLIssue -> error("Cannot validate schema: $it")
              else -> error("Unexpected issue type: ${it::class.simpleName}")
            }
          }
          it.value!!
        }
        break
      }
      parent = parent.parentFile
    }
    check(schema != null) {
      "Cannot find a schema for $graphQLFile"
    }

    val actual = block(schema)

    checkExpected(graphQLFile) {
      actual
    }
  }
  class ParametersProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<File> {
      return findFiles("test-fixtures/validation/executable")
          .filter { it.name.endsWith(".graphql") }
    }
  }
}
