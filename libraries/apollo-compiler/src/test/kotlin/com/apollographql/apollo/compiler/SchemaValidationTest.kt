package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.validateAsSchemaAndAddApolloDefinition
import com.apollographql.apollo.compiler.TestUtils.checkTestFixture
import com.apollographql.apollo.compiler.TestUtils.serialize
import com.apollographql.apollo.compiler.TestUtils.testFilterMatches
import com.apollographql.apollo.compiler.internal.checkApolloReservedEnumValueNames
import com.apollographql.apollo.compiler.internal.checkApolloTargetNameClashes
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class SchemaValidationTest(name: String, private val graphqlsFile: File) {

  @Test
  fun testValidation() {
    val parseResult = graphqlsFile.source().buffer().parseAsGQLDocument(graphqlsFile.name)

    val issues = if (parseResult.issues.isNotEmpty()) {
      parseResult.issues
    } else {
      val schemaResult = parseResult.getOrThrow().validateAsSchemaAndAddApolloDefinition()
      schemaResult.issues +
          (schemaResult.value?.let { checkApolloReservedEnumValueNames(it) } ?: emptyList()) +
          (schemaResult.value?.let { checkApolloTargetNameClashes(it) } ?: emptyList())
    }

    val actualContents = issues.serialize()
    val expected = graphqlsFile.parentFile.resolve(graphqlsFile.nameWithoutExtension + ".expected")

    checkTestFixture(actualContents, expected)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() = File("src/test/validation/schema")
        .walk()
        .toList()
        .filter { it.isFile }
        .filter { it.extension == "graphqls" }
        .filter { testFilterMatches(it.name) }
        .sortedBy { it.name }
        .map { arrayOf(it.nameWithoutExtension, it) }
  }
}
