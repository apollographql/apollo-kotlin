@file:JvmName("-FileSystemCommon")
package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.jsonReader
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.jvm.JvmName

/**
 * Checks that [actualText] matches the contents of the file at [path]
 *
 * If it doesn't and [shouldUpdateTestFixtures] is true, update the contents, else throw
 */
@OptIn(ApolloExperimental::class)
fun checkFile(actualText: String, path: String) {
  val updateTestFixtures = shouldUpdateTestFixtures()
  val expectedText = try {
    HostFileSystem.openReadOnly(path.toPath()).source().buffer().readUtf8()
  } catch (e: IOException) {
    if (updateTestFixtures) {
      // do not throw here as we can update the fixtures below
      println("$path is not found")
      ""
    } else {
      throw e
    }
  }

  val expected = "testFixtures/$path".toPath()

  if (actualText != expectedText) {
    if (updateTestFixtures) {
      HostFileSystem.openReadWrite(expected).sink().buffer().writeUtf8(actualText)
    } else {
      throw Exception("""generatedText doesn't match the expectedText.
      |If you changed the compiler recently, you need to update the testFixtures.
      |Run the tests with `-DupdateTestFixtures=true` to do so.
      |generatedText: $actualText
      |expectedText : $expectedText""".trimMargin())
    }
  }
}

fun pathToUtf8(path: String): String {
  return HostFileSystem.openReadOnly(path.toPath()).source().buffer().readUtf8()
}

fun pathToJsonReader(path: String): JsonReader {
  return HostFileSystem.openReadOnly(path.toPath()).source().buffer().jsonReader()
}
