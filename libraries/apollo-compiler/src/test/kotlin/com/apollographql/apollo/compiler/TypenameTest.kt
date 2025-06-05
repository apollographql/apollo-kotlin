package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.toExecutableDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.apollo.compiler.internal.ApolloExecutableDocumentTransform
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class TypenameTest(
    @TestParameter(valuesProvider = GraphQLFileValuesProvider::class) private val graphQLFile: File,
    @TestParameter("always", "ifFragments", "ifAbstract", "ifPolymorphic") private val addTypename: String,
) {
  @Test
  fun testTypename() {
    val schemaFile = File("src/test/graphql/schema.graphqls")
    val schema = schemaFile.toGQLDocument().toSchema()

    val document = graphQLFile.source().buffer().toExecutableDocument(schema)

    val documentWithTypename = ApolloExecutableDocumentTransform(addTypename, false).transform(schema, document, emptyList()).toUtf8()

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
