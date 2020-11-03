package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.error.DocumentParseException
import com.apollographql.apollo.compiler.parser.error.ParseException
import com.apollographql.apollo.compiler.parser.graphql.ast.GQLDocument
import com.apollographql.apollo.compiler.parser.graphql.ast.fromFile
import com.apollographql.apollo.compiler.parser.sdl.GraphSdlSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class SDLValidationTest(name: String, private val sdlFile: File) {

  @Test
  fun testValidation() {
    try {
      GQLDocument.fromFile(sdlFile)
      fail("parse expected to fail but was successful")
    } catch (e: Exception) {
      if (e is DocumentParseException || e is ParseException) {
        val expected = File(sdlFile.parent, sdlFile.nameWithoutExtension + ".error").readText().removeSuffix("\n")
        val actual = e.message!!.removePrefix("\n").removeSuffix("\n").replace(sdlFile.absolutePath, "/${sdlFile.name}")
        assertThat(actual).isEqualTo(expected)
      } else {
        throw e
      }
    }
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      return File("src/test/validation/sdl")
          .listFiles()!!
          .filter { it.extension == "sdl" }
          .map { arrayOf(it.nameWithoutExtension, it) }
    }
  }
}
