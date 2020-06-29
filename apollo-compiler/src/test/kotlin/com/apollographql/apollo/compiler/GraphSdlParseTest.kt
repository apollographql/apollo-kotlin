package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.sdl.GraphSDLSchemaParser.parse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class GraphSdlParseTest(name: String, private val schemaFile: File) {

  @Test
  fun testParseSuccessfully() {
    schemaFile.parse()
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      return File("src/test/sdl")
          .listFiles()!!
          .filter { it.extension == "graphql" }
          .map { arrayOf(it.nameWithoutExtension, it) }
    }
  }
}
