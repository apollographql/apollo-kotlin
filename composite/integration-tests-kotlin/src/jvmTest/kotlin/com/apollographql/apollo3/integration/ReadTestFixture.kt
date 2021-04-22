package com.apollographql.apollo3.integration

import java.io.File
import java.io.FileNotFoundException

actual fun readTestFixture(name: String): String {
  return File("../integration-tests/testFixtures/$name").readText()
}

actual fun checkTestFixture(actualText: String, name: String) {
  val expectedText = try {
    readTestFixture(name)
  } catch (e: FileNotFoundException) {
    println("$name is not found do not throw here as we can update the fixtures below")
    ""
  }

  val expected = File("../integration-tests/testFixtures/$name")
  expected.parentFile.mkdirs()
  if (actualText != expectedText) {
    when (System.getProperty("updateTestFixtures")?.trim()) {
      "on", "true", "1" -> {
        expected.writeText(actualText)
      }
      else -> {
        throw java.lang.Exception("""generatedText doesn't match the expectedText.
      |If you changed the compiler recently, you need to update the testFixtures.
      |Run the tests with `-DupdateTestFixtures=true` to do so.
      |generatedText: $actualText
      |expectedText : $expectedText""".trimMargin())
      }
    }
  }
}