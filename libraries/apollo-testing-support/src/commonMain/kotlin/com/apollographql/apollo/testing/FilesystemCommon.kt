@file:JvmName("-FileSystemCommon")

package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.jsonReader
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.jvm.JvmName

/**
 * Checks that [actualText] matches the contents of the file at [path]
 *
 * If it doesn't and [shouldUpdateTestFixtures] is true, update the contents, else throw
 *
 * @param path: the path to the file, from the "tests" directory
 */
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun checkFile(actualText: String, path: String) {
  @Suppress("DEPRECATION")
  val updateTestFixtures = shouldUpdateTestFixtures()
  val expected = path.toTestsPath()
  val expectedText = try {
    @Suppress("DEPRECATION")
    HostFileSystem.openReadOnly(expected).source().buffer().readUtf8()
  } catch (e: IOException) {
    if (updateTestFixtures) {
      // do not throw here as we can update the fixtures below
      println("$path is not found")
      ""
    } else {
      throw e
    }
  }

  if (actualText != expectedText) {
    if (updateTestFixtures) {
      @Suppress("DEPRECATION")
      HostFileSystem.delete(expected)
      @Suppress("DEPRECATION")
      HostFileSystem.openReadWrite(
          file = expected,
      ).use {
        it.sink().buffer().use {
          it.writeUtf8(actualText)
        }
      }
    } else {
      throw Exception("""generatedText doesn't match the expectedText.
      |If you changed the compiler recently, you need to update the testFixtures.
      |Run the tests with `updateTestFixtures=true` as an environment variable
      |to do so (updateTestFixtures=true ./gradlew jvmTest ...).
      |generatedText: $actualText
      |expectedText : $expectedText""".trimMargin())
    }
  }
}

private fun String.toTestsPath(): Path {
  @Suppress("DEPRECATION")
  return testsPath.toPath().resolve(this.toPath())
}

/**
 * @param path: the path to the file, from the "tests" directory
 */
@Deprecated("This function is not Apollo specific and will be removed in a future version. Copy/paste it in your codebase if you need it")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun pathToUtf8(path: String): String {
  @Suppress("DEPRECATION")
  return HostFileSystem.openReadOnly(path.toTestsPath()).source().buffer().readUtf8()
}

/**
 * @param path: the path to the file, from the "tests" directory
 */
@Deprecated("This function is not Apollo specific and will be removed in a future version. Copy/paste it in your codebase if you need it")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun pathToJsonReader(path: String): JsonReader {
  @Suppress("DEPRECATION")
  return HostFileSystem.openReadOnly(path.toTestsPath()).source().buffer().jsonReader()
}
