package com.apollographql.apollo3.testing

import java.io.File
import java.io.FileNotFoundException

actual fun readFile(path: String): String {
  return File(path).readText()
}

actual fun checkFile(actualText: String, path: String) {
  val updateTestFixtures =  when (System.getProperty("updateTestFixtures")?.trim()) {
    "on", "true", "1" -> true
    else -> false
  }
  val expectedText = try {
    readFile(path)
  } catch (e: FileNotFoundException) {
    if (updateTestFixtures) {
      // do not throw here as we can update the fixtures below
      println("$path is not found")
      ""
    } else {
      throw e
    }
  }

  val expected = File("../integration-tests/testFixtures/$path")
  expected.parentFile.mkdirs()
  if (actualText != expectedText) {
    if (updateTestFixtures) {
      expected.writeText(actualText)
    } else {
      throw java.lang.Exception("""generatedText doesn't match the expectedText.
      |If you changed the compiler recently, you need to update the testFixtures.
      |Run the tests with `-DupdateTestFixtures=true` to do so.
      |generatedText: $actualText
      |expectedText : $expectedText""".trimMargin())
    }
  }
}
