package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.TestUtils.checkExpected
import com.apollographql.apollo3.compiler.TestUtils.testParametersForGraphQLFilesIn
import com.apollographql.apollo3.graphql.ast.Issue
import com.apollographql.apollo3.graphql.ast.ParseResult
import com.apollographql.apollo3.graphql.ast.parseAsGraphQLDocument
import com.apollographql.apollo3.graphql.ast.validateAsOperations
import com.apollographql.apollo3.graphql.ast.validateAsSchema
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
    val issues = if (graphQLFile.parentFile.name == "operation") {
      val parseResult = graphQLFile.parseAsGraphQLDocument()

      when (parseResult) {
        is ParseResult.Error -> parseResult.issues
        is ParseResult.Success -> parseResult.value.validateAsOperations(schema!!)
      }
    } else {
      val parseResult = graphQLFile.parseAsGraphQLDocument()

      when (parseResult) {
        is ParseResult.Error -> parseResult.issues
        is ParseResult.Success -> parseResult.value.validateAsSchema()
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
