package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.validateAsExecutable
import com.apollographql.apollo.ast.validateAsSchemaAndAddApolloDefinition
import com.apollographql.apollo.compiler.TestUtils.checkExpected
import com.apollographql.apollo.compiler.TestUtils.serialize
import com.apollographql.apollo.compiler.TestUtils.testParametersForGraphQLFilesIn
import com.apollographql.apollo.compiler.internal.checkApolloReservedEnumValueNames
import com.apollographql.apollo.compiler.internal.checkApolloTargetNameClashes
import com.apollographql.apollo.compiler.internal.checkCapitalizedFields
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class ValidationTest(name: String, private val graphQLFile: File) {

  @Test
  fun testValidation() = checkExpected(graphQLFile) { schema ->
    // We just pass graphQLFile.name here to keep the issues shorter
    val parseResult = graphQLFile.source().buffer().parseAsGQLDocument(graphQLFile.name)

    val issues = if (graphQLFile.parentFile.name == "operation" || graphQLFile.parentFile.parentFile.name == "operation") {
      if (parseResult.issues.isNotEmpty()) {
        parseResult.issues
      } else {
        parseResult.getOrThrow().validateAsExecutable(schema = schema!!).issues +
            if (graphQLFile.name == "capitalized_fields_disallowed.graphql") {
              checkCapitalizedFields(parseResult.value!!.definitions, checkFragmentsOnly = false)
            } else {
              emptyList()
            } +
            if (graphQLFile.name == "capitalized_fields_allowed_with_fragment_spread.graphql") {
              checkCapitalizedFields(parseResult.value!!.definitions, checkFragmentsOnly = true)
            } else {
              emptyList()
            }
      }
    } else {
      if (parseResult.issues.isNotEmpty()) {
        parseResult.issues
      } else {
        val schemaResult = parseResult.getOrThrow().validateAsSchemaAndAddApolloDefinition()
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
