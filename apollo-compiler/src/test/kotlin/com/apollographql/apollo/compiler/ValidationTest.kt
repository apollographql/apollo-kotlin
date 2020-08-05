package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.error.DocumentParseException
import com.apollographql.apollo.compiler.parser.error.ParseException
import com.apollographql.apollo.compiler.parser.graphql.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
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
    val schema = IntrospectionSchema(schemaFile)
    val packageNameProvider = DefaultPackageNameProvider.of(
        rootFolders = listOf(graphQLFile.parentFile),
        schemaPackageName = "",
        rootPackageName = ""
    )

    try {
      GraphQLDocumentParser(schema, packageNameProvider, exportAllTypes = false).parse(setOf(graphQLFile))
      fail("parse expected to fail but was successful")
    } catch (e: Exception) {
      if (e is DocumentParseException || e is ParseException) {
        val expected = File(graphQLFile.parent, graphQLFile.nameWithoutExtension + ".error").readText().removeSuffix("\n")
        val actual = e.message!!.removePrefix("\n").removeSuffix("\n").replace(graphQLFile.absolutePath, "/${graphQLFile.name}")
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
      return File("src/test/validation/graphql")
          .listFiles()!!
          .filter { it.extension == "graphql" }
          .map { arrayOf(it.nameWithoutExtension, it) }
    }
  }
}
