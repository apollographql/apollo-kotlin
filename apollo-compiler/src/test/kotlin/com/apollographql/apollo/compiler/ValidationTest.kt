package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.frontend.GraphQLParser
import com.apollographql.apollo.compiler.frontend.Issue
import com.apollographql.apollo.compiler.frontend.toSchema
import com.apollographql.apollo.compiler.introspection.IntrospectionSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class ValidationTest(name: String, private val graphQLFile: File) {
  private val separator = "\n------------\n"

  private fun List<Issue>.serialize() = map {
    "${it.severity}: ${it.javaClass.simpleName} (${it.sourceLocation.line}:${it.sourceLocation.position})\n${it.message}"
  }.joinToString(separator)

  @Test
  fun testValidation() {
    val issues = if (graphQLFile.parentFile.name == "operation") {
      val schemaFile = File("src/test/validation/schema.json")
      val schema = IntrospectionSchema(schemaFile).toSchema()
      GraphQLParser.parseOperations(graphQLFile, schema).issues
    } else {
      GraphQLParser.parseSchemaInternal(graphQLFile).issues
    }

    val expectedIssuesFile = File(graphQLFile.parent, graphQLFile.nameWithoutExtension + ".issues")
    val expectedIssues = try {
      expectedIssuesFile.readText()
    } catch (e: Exception) {
      null
    }

    val actualIssues = issues.serialize()

    if (TestUtils.shouldUpdateTestFixtures()) {
      expectedIssuesFile.writeText(actualIssues)
    } else {
      assertThat(actualIssues).isEqualTo(expectedIssues)
    }
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      return File("src/test/validation/")
          .walk()
          .toList()
          .filter { it.isFile }
          .filter { it.extension == "graphql" }
          .sortedBy { it.name }
          //.filter { it.name.contains("InputObjectFieldType") }
          .map { arrayOf(it.nameWithoutExtension, it) }
    }
  }
}
