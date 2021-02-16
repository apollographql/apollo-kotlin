package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.TestUtils.checkExpected
import com.apollographql.apollo3.compiler.TestUtils.testParametersForGraphQLFilesIn
import com.apollographql.apollo3.compiler.frontend.GraphQLParser
import com.apollographql.apollo3.compiler.frontend.Issue
import com.apollographql.apollo3.compiler.frontend.toSchema
import com.apollographql.apollo3.compiler.introspection.IntrospectionSchema
import com.google.common.truth.Truth.assertThat
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
      GraphQLParser.parseOperations(graphQLFile, schema!!).issues
    } else {
      // schema is unused there
      GraphQLParser.parseSchemaInternal(graphQLFile).issues
    }
    issues.serialize()
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() = testParametersForGraphQLFilesIn("src/test/validation/")
  }
}
