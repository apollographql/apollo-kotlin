package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.gql.GQLFragmentDefinition
import com.apollographql.apollo.compiler.parser.gql.GQLOperationDefinition
import com.apollographql.apollo.compiler.parser.gql.GraphQLParser
import com.apollographql.apollo.compiler.parser.gql.toFile
import com.apollographql.apollo.compiler.parser.gql.toSchema
import com.apollographql.apollo.compiler.parser.gql.toUtf8
import com.apollographql.apollo.compiler.parser.gql.toUtf8WithIndents
import com.apollographql.apollo.compiler.parser.gql.withTypenameWhenNeeded
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
class TypenameTest(val name: String, private val graphQLFile: File) {

  @Test
  fun testTypename() {
    val schemaFile = File("src/test/graphql/schema.sdl")
    val schema = GraphQLParser.parseSchema(schemaFile)

    val document = GraphQLParser.parseOperations(graphQLFile, schema).orThrow()

    val documentWithTypename = document.copy(
        definitions = document.definitions.map {
          when (it) {
            is GQLOperationDefinition -> it.withTypenameWhenNeeded(schema)
            is GQLFragmentDefinition -> it.withTypenameWhenNeeded(schema)
            else -> it
          }
        }
    ).toUtf8WithIndents()

    val expectedFile = File(graphQLFile.parentFile, "${name}.with_typename")


    if (TestUtils.shouldUpdateTestFixtures()) {
      expectedFile.writeText(documentWithTypename)
    } else {
      assertThat(documentWithTypename).isEqualTo(expectedFile.readText())
    }
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      return File("src/test/typename/")
          .walk()
          .toList()
          .filter { it.isFile }
          .filter { it.extension == "graphql" }
          .sortedBy { it.name }
          .map { arrayOf(it.nameWithoutExtension, it) }
    }
  }
}
