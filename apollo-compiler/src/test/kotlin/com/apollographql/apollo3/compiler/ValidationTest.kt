package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.validateAsExecutable
import com.apollographql.apollo3.ast.validateAsSchemaAndAddApolloDefinition
import com.apollographql.apollo3.compiler.TestUtils.checkExpected
import com.apollographql.apollo3.compiler.TestUtils.findSchema
import com.apollographql.apollo3.compiler.TestUtils.testParametersForGraphQLFilesIn
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.assertTrue

@Suppress("UNUSED_PARAMETER")
@RunWith(Enclosed::class)
class ValidationTest {

  @RunWith(Parameterized::class)
  class ValidationParamTest(name: String, private val graphQLFile: File) {
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
          parseResult.valueAssertNoErrors().validateAsExecutable(schema = schema!!, fieldOnDisjointTypesMustMerge = false).issues + if (graphQLFile.name == "capitalized_fields_disallowed.graphql") {
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
          schemaResult.issues +
              (schemaResult.value?.let { checkApolloReservedEnumValueNames(it) } ?: emptyList()) +
              (schemaResult.value?.let { checkApolloTargetNameClashes(it) } ?: emptyList())
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

  @Test
  fun testFieldOnDisjointTypesMustMerge() {
    val file = File("src/test/validation/operation/fields_in_set_can_merge/different_shapes.graphql")
    val schema = findSchema(file.parentFile)!!
    val parseResult = file.source().buffer().parseAsGQLDocument(filePath = file.name)

    val issues = parseResult.valueAssertNoErrors().validateAsExecutable(schema = schema, fieldOnDisjointTypesMustMerge = true).issues

    assertTrue(issues.isEmpty())
  }
}
