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
    val actualText = actual.readText()
    val expectedText = expected.readText()

    if (actualText != expectedText) {
      if (shouldUpdateTestFixtures()) {
        expected.writeText(actualText)
      } else {
        throw Exception("""generatedFile content doesn't match the expectedFile content.
      |If you changed the compiler recently, you need to update the testFixtures.
      |Run the tests with `-DupdateTestFixtures=true` to do so.
      |generatedFile: ${actual.path}
      |expectedFile: ${expected.path}""".trimMargin())
      }
    }
  }
}