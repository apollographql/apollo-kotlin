package com.apollographql.apollo.compiler

import java.io.File
import java.lang.Exception

internal object TestUtils {
  internal fun shouldUpdateTestFixtures(): Boolean {
    return when (System.getProperty("updateTestFixtures")?.trim()) {
      "on", "true", "1" -> true
      else -> false
    }
  }

  internal fun checkTestFixture(actual: File, expected: File) {
    return checkTestFixture(actual.readText(), expected, actual.path)
  }

  internal fun checkTestFixture(actualText: String, expected: File, source: String = "(source)") {
    val expectedText = expected.readText()

    if (actualText != expectedText) {
      if (shouldUpdateTestFixtures()) {
        expected.writeText(actualText)
      } else {
        throw Exception("""generatedFile content doesn't match the expectedFile content.
      |If you changed the compiler recently, you need to update the testFixtures.
      |Run the tests with `-DupdateTestFixtures=true` to do so.
      |diff ${expected.path} ${source}""".trimMargin())
      }
    }
  }
}