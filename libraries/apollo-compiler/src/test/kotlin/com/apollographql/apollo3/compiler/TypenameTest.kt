package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.toExecutableDefinitions
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.transformation.addRequiredFields
import com.apollographql.apollo3.ast.introspection.toSchema
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@Suppress("UNUSED_PARAMETER")
@RunWith(TestParameterInjector::class)
class TypenameTest(
    @TestParameter(valuesProvider = GraphQLFileValuesProvider::class) private val graphQLFile: File,
    @TestParameter("always", "ifFragments", "ifAbstract", "ifPolymorphic") private val addTypename: String,
) {

  @Test
  fun testTypename() {
    val schemaFile = File("src/test/graphql/schema.sdl")
    val schema = schemaFile.toSchema()

    val definitions = graphQLFile.source().buffer().toExecutableDefinitions(schema)

    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }
    val documentWithTypename = GQLDocument(
        definitions = definitions.map {
          when (it) {
            is GQLOperationDefinition -> addRequiredFields(it, addTypename, schema, fragments)
            is GQLFragmentDefinition -> addRequiredFields(it, addTypename, schema, fragments)
            else -> it
          }
        },
        filePath = null
    ).toUtf8()

    val extra = when(addTypename) {
      "ifFragments" -> "" // for backward compat
      else -> ".$addTypename"
    }

    val expectedFile = File(graphQLFile.parentFile, "${graphQLFile.nameWithoutExtension}.with_typename$extra")

    if (TestUtils.shouldUpdateTestFixtures()) {
      expectedFile.writeText(documentWithTypename)
    } else {
      assertThat(documentWithTypename).isEqualTo(expectedFile.readText())
    }
  }

  class GraphQLFileValuesProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues(): List<File> {
      return File("src/test/typename/")
          .walk()
          .toList()
          .filter { it.isFile }
          .filter { it.extension == "graphql" }
          .sortedBy { it.name }
    }
  }
}
