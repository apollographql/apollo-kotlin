package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.introspection.toGQLDocument
import com.apollographql.apollo.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo.ast.validateAsSchema
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class IntrospectionTest {
  @Test
  fun canReadIntrospectionResults(@TestParameter(valuesProvider = ParametersProvider::class) graphqlFile: File) {
    graphqlFile
        .toIntrospectionSchema()
        .toGQLDocument()
        .validateAsSchema()
        .getOrThrow()
  }

  class ParametersProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<File> {
      return File("test-fixtures/introspection/")
          .listFiles()!!
          .filter { it.name.endsWith(".json") }
          .filter {
            testFilterMatches(it.name)
          }
    }
  }
}