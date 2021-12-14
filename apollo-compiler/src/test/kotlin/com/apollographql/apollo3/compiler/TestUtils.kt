package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.compiler.introspection.toSchema
import com.google.common.truth.Truth.assertThat
import okio.Buffer
import java.io.File

@OptIn(ApolloExperimental::class)
internal object TestUtils {
  internal fun shouldUpdateTestFixtures(): Boolean {
    return when (System.getProperty("updateTestFixtures")?.trim()) {
      "on", "true", "1" -> true
      else -> false
    }
  }
  internal fun shouldUpdateMeasurements(): Boolean {
    return when (System.getProperty("updateTestFixtures")?.trim()) {
      "on", "true", "1" -> true
      else -> false
    }
  }

  internal fun checkTestFixture(actual: File, expected: File) {
    val actualText = actual.readText()
    val expectedText = expected.readText()

    if (actualText != expectedText) {
      if (shouldUpdateTestFixtures()) {
        expected.writeText(actualText)
      } else {
        throw Exception("""generatedFile content doesn't match the expectedFile content.
      |If you changed the compiler recently, you need to update the testFixtures.
      |Run the tests with `-DupdateTestFixtures=true` to do so.
      |diff ${expected.path} ${actual.path}""".trimMargin())
      }
    }
  }

  /**
   * This allows to run a specific test from the command line by using something like:
   *
   * ./gradlew :apollo-compiler:test -testFilter="fragments_with_type_condition" --tests '*Codegen*'
   */
  fun testFilterMatches(value: String): Boolean {
    val testFilter = System.getProperty("testFilter") ?: return true

    return Regex(testFilter).containsMatchIn(value)
  }
  fun testParametersForGraphQLFilesIn(path: String): Collection<Array<Any>> {
    return File(path)
        .walk()
        .toList()
        .filter { it.isFile }
        .filter { it.extension == "graphql" }
        .filter {
          testFilterMatches(it.name)
        }
        .sortedBy { it.name }
        .map { arrayOf(it.nameWithoutExtension, it) }
  }

  private fun File.replaceExtension(newExtension: String): File {
    return File(parentFile, "$nameWithoutExtension.$newExtension")
  }

  private fun findSchema(dir: File): Schema? {
    return listOf("graphqls", "sdl", "json").map { File(dir, "schema.$it") }
        .firstOrNull { it.exists() }
        ?.let {
          it.toSchema()
        }
  }

  /**
   * run the block and checks the result against the .expected file
   *
   * @param block: the callback to produce the result. [checkExpected] will try to find a schema
   * for [graphQLFile] by either looking for a schema with the same name or testing the first
   * schema.[json|sdl|graphqls] in the hierarchy
   */
  fun checkExpected(graphQLFile: File, block: (Schema?) -> String) {
    var parent = graphQLFile.parentFile

    // We're in src/test/validation/operation/...
    // There's a schema at src/test/validation/schema.json if we go past that, stop
    var schema: Schema? = null
    while (parent.name != "test") {
      schema = findSchema(parent)
      if (schema != null) {
        break
      }
      parent = parent.parentFile
    }
    check (schema != null) {
      "Cannot find a schema for $graphQLFile"
    }

    val actual = block(schema)

    val expectedFile = File(graphQLFile.parent, graphQLFile.nameWithoutExtension + ".expected")
    val expected = try {
      expectedFile.readText()
    } catch (e: Exception) {
      null
    }

    if (shouldUpdateTestFixtures()) {
      expectedFile.writeText(actual)
    } else {
      assertThat(actual).isEqualTo(expected)
    }
  }
}

internal fun String.buffer() = Buffer().writeUtf8(this)