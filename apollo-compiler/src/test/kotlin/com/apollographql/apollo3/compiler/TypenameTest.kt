package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.toExecutableDefinitions
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.transformation.addRequiredFields
import com.apollographql.apollo3.compiler.introspection.toSchema
import com.google.common.truth.Truth.assertThat
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
@OptIn(ApolloExperimental::class)
class TypenameTest(val name: String, private val graphQLFile: File) {

  @Test
  fun testTypename() {
    val schemaFile = File("src/test/graphql/schema.sdl")
    val schema = schemaFile.toSchema()

    val definitions = graphQLFile.source().buffer().toExecutableDefinitions(schema)
    val fragmentDefinitions = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }
    val documentWithTypename = GQLDocument(
        definitions = definitions.map {
          when (it) {
            is GQLOperationDefinition -> addRequiredFields(it, schema, fragmentDefinitions)
            is GQLFragmentDefinition -> addRequiredFields(it, schema, fragmentDefinitions)
            else -> it
          }
        },
        filePath = null
    ).toUtf8()

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
