package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLResult
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.MergeOptions
import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.SchemaValidationOptions
import com.apollographql.apollo.ast.toUtf8
import java.io.File
import kotlin.test.assertEquals

private const val separator = "\n------------\n"

private fun List<Issue>.serialize() = joinToString(separator) {
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

fun shouldUpdateTestFixtures(): Boolean {
  if (System.getenv("updateTestFixtures") != null) {
    return true
  }

  return false
}

internal fun testFilterMatches(value: String): Boolean {
  val testFilter = System.getenv("testFilter") ?: return true

  return Regex(testFilter).containsMatchIn(value)
}

/**
 * Run the block and checks the result against the .expected file. Will overwrite the result if required
 *
 * @param block the callback to produce the result.
 */
internal fun checkExpected(graphQLFile: File, block: (File) -> String) {
  val actual = block(graphQLFile)

  val expectedFile = File(graphQLFile.parent, graphQLFile.nameWithoutExtension + ".expected.graphql")
  val expected = try {
    expectedFile.readText()
  } catch (e: Exception) {
    null
  }

  if (shouldUpdateTestFixtures()) {
    expectedFile.writeText(actual)
  } else {
    assertEquals(expected, actual)
  }
}

enum class Pragma {
  allowMergingFieldDefinitions,
  allowDirectivesOnDirectives,
  allowServiceCapabilities,
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
      }
      .build()
}

fun List<Pragma>.toMergerOptions(): MergeOptions {
  return MergeOptions(Pragma.allowMergingFieldDefinitions in this)
}