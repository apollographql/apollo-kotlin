package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.ParseResult
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.validateAsExecutable
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.compiler.TestUtils.checkExpected
import com.apollographql.apollo3.compiler.TestUtils.testParametersForGraphQLFilesIn
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
      val parseResult = graphQLFile.parseAsGQLDocument()

      when (parseResult) {
        is ParseResult.Error -> parseResult.issues
        is ParseResult.Success -> parseResult.value.validateAsExecutable(schema!!)
      }
    } else {
      val parseResult = graphQLFile.parseAsGQLDocument()

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
