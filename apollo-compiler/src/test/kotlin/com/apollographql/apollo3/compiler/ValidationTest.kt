package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.validateAsExecutable
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.ast.validateAsSchemaAndAddApolloDefinition
import com.apollographql.apollo3.compiler.TestUtils.checkExpected
import com.apollographql.apollo3.compiler.TestUtils.testParametersForGraphQLFilesIn
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class ValidationTest(name: String, private val graphQLFile: File) {
  private val separator = "\n------------\n"

  private fun List<Issue>.serialize() = joinToString(separator) {
    "${it.severity}: ${it.javaClass.simpleName} (${it.sourceLocation.line}:${it.sourceLocation.position})\n${it.message}"
  }

  @Test
  fun testValidation() = checkExpected(graphQLFile) { schema ->
    // Don't use absolute path for filePath because it depends on the machine where the test is run
    val parseResult = graphQLFile.source().buffer().parseAsGQLDocument(filePath = graphQLFile.name)

    val issues = if (graphQLFile.parentFile.name == "operation" || graphQLFile.parentFile.parentFile.name == "operation") {
      if (parseResult.issues.isNotEmpty()) {
        parseResult.issues
      } else {
        parseResult.valueAssertNoErrors().validateAsExecutable(schema = schema!!).issues + if (graphQLFile.name == "capitalized_fields_disallowed.graphql") {
          checkCapitalizedFields(parseResult.value!!.definitions)
        } else {
          emptyList()
        }
      }
    } else {
      if (parseResult.issues.isNotEmpty()) {
        parseResult.issues
      } else {
        val schemaResult = parseResult.valueAssertNoErrors().validateAsSchemaAndAddApolloDefinition()
        schemaResult.issues + if (graphQLFile.name == "reserved-enum-value-names.graphql") {
          checkApolloReservedEnumValueNames(schemaResult.value!!)
        } else {
          emptyList()
        }
      }
    }
    issues.serialize()
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() = testParametersForGraphQLFilesIn("src/test/validation/")
  }
}
