package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.parseAsGQLDocument
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class ParserTest {
  @Test
  fun test(@TestParameter(valuesProvider = ParametersProvider::class) graphqlFile: File) {
    checkExpected(graphqlFile) {
      val pragmas = it.pragmas()

      it.parseAsGQLDocument(pragmas.toParserOptions()).serialize()
    }
  }

  class ParametersProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<File> {
      return File("test-fixtures/parser/")
          .listFiles()!!
          .filter { it.name.endsWith(".graphql") && !it.name.endsWith(".expected.graphql") }
          .sortedBy { it.name }
          .filter {
            testFilterMatches(it.name)
          }
    }
  }
}