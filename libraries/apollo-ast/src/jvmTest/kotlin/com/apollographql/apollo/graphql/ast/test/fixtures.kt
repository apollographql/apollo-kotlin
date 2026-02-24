package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLResult
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.MergeOptions
import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.SchemaValidationOptions
import com.apollographql.apollo.ast.builtinForeignSchemas
import com.apollographql.apollo.ast.toUtf8
import java.io.File
import kotlin.test.assertEquals

private const val separator = "\n------------\n"

internal fun List<Issue>.serialize() = joinToString(separator) {
  "${it.javaClass.simpleName} (${it.sourceLocation?.line}:${it.sourceLocation?.column})\n${it.message}"
}

internal fun GQLResult<GQLDocument>.serialize(): String {
  return buildString {
    if (issues.isNotEmpty()) {
      append(issues.serialize())
    }
    if (value != null) {
      append("\n")
      append(value.toUtf8())
    }
  }
}

fun shouldUpdateTestFixtures() = System.getenv("updateTestFixtures") != null

internal fun testFilterMatches(value: String): Boolean {
  val testFilter = System.getenv("testFilter") ?: return true

  return Regex(testFilter).containsMatchIn(value)
}

/**
 * Run the block and checks the result against the .expected.graphql file. Will overwrite the result if required
 *
 * @param block the callback to produce the result.
 */
internal fun checkExpected(graphQLFile: File, block: (File) -> String) {
  val actual = block(graphQLFile)

  val expectedFile = File(graphQLFile.parent, graphQLFile.nameWithoutExtension + ".expected")
  val expected = try {
    expectedFile.readText()
  } catch (e: Exception) {
    null
  }

  if (shouldUpdateTestFixtures()) {
    expectedFile.writeText(actual)
  } else {
    assertEquals(expected, actual, "The expected results do not match the test data. If this is expected, re-run the test with `updateTestFixtures=true` as an environment variable.")
  }
}

internal fun findFiles(path: String): List<File> {
  return File(path)
      .walk()
      .filter {
        testFilterMatches(it.name)
      }
      .toList()
      .sortedBy { it.name }
}

enum class Pragma {
  // Parser
  allowDirectivesOnDirectives,
  allowServiceCapabilities,
  // Merger
  allowMergingFieldDefinitions,
  // Schema validation
  addBuiltinForeignSchemas,
  addKotlinLabsDefinitions,
  addBuiltinDefinitions,
  noAddBuiltinDefinitions,
  // Executable validation
  allowDirectiveRedefinition,
  // Parser and validation
  allowFragmentArguments,
}

fun File.pragmas(): List<Pragma> =
  readText().lines().filter { it.startsWith("# PRAGMA ") }.map { it.removePrefix("# PRAGMA ").trim() }.map { Pragma.valueOf(it) }

fun List<Pragma>.toParserOptions(): ParserOptions {
  return ParserOptions.Builder()
      .apply {
        if (Pragma.allowServiceCapabilities in this@toParserOptions) {
          allowServiceCapabilities(true)
        }
        if (Pragma.allowDirectivesOnDirectives in this@toParserOptions) {
          allowDirectivesOnDirectives(true)
        }
        if (Pragma.allowFragmentArguments in this@toParserOptions) {
          allowFragmentArguments(true)
        }
      }
      .build()
}

fun List<Pragma>.toMergerOptions(): MergeOptions {
  return MergeOptions(Pragma.allowMergingFieldDefinitions in this)
}

fun List<Pragma>.toSchemaValidationOptions(): SchemaValidationOptions {
  return SchemaValidationOptions.Builder()
      .apply {
        if (Pragma.addBuiltinForeignSchemas in this@toSchemaValidationOptions) {
          foreignSchemas(builtinForeignSchemas())
        }
        if (Pragma.addKotlinLabsDefinitions in this@toSchemaValidationOptions) {
          addKotlinLabsDefinitions(true)
        }
        if (Pragma.addBuiltinDefinitions in this@toSchemaValidationOptions) {
          addBuiltinDefinitions(true)
        }
        if (Pragma.noAddBuiltinDefinitions in this@toSchemaValidationOptions) {
          addBuiltinDefinitions(false)
        }
      }
      .build()
}