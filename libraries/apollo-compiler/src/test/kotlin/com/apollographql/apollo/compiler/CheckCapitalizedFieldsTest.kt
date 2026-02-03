package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.compiler.TestUtils.checkExpected
import com.apollographql.apollo.compiler.TestUtils.serialize
import com.apollographql.apollo.compiler.TestUtils.testParametersForGraphQLFilesIn
import com.apollographql.apollo.compiler.internal.checkCapitalizedFields
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class CheckCapitalizedFieldsTest(name: String, private val graphQLFile: File) {

  @Test
  fun testValidation() = checkExpected(graphQLFile) { _ ->
    val checkFragmentsOnly = graphQLFile.name != "capitalized_fields_disallowed.graphql"

    checkCapitalizedFields(graphQLFile.toGQLDocument().definitions, checkFragmentsOnly = checkFragmentsOnly).serialize()
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() = testParametersForGraphQLFilesIn("src/test/validation/operation")
  }
}
