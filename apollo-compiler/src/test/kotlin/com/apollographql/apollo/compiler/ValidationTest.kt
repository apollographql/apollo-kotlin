package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.GraphQLDocumentParseException
import com.apollographql.apollo.compiler.parser.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.Schema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class ValidationTest(name: String, private val graphQLFile: File) {

  @Test
  fun testValidation() {
    val schemaFile = File("src/test/validation/schema.json")
    val schema = Schema(schemaFile)
    val packageNameProvider = DefaultPackageNameProvider(
        rootFolders = emptyList(),
        schemaFile = schemaFile,
        rootPackageName = ""
    )

    try {
      GraphQLDocumentParser(schema, packageNameProvider).parse(setOf(graphQLFile))
    } catch (e: GraphQLDocumentParseException) {
      val expected = File(graphQLFile.parent, graphQLFile.nameWithoutExtension + ".error").readText().removeSuffix("\n")
      val actual = e.message!!.removePrefix("\n").removeSuffix("\n").replace(graphQLFile.absolutePath, "/${graphQLFile.name}")
      assertThat(actual).isEqualTo(expected)
    }
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      return File("src/test/validation/graphql")
          .listFiles()!!
          .filter { it.extension == "graphql" }
          .map { arrayOf(it.nameWithoutExtension, it) }
    }
  }
}
