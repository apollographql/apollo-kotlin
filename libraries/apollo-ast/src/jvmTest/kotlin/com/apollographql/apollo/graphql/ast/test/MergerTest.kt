package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.mergeExtensions
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.Test

@RunWith(TestParameterInjector::class)
class MergerTest {
  @Test
  fun test(@TestParameter(valuesProvider = ParametersProvider::class) graphqlFile: File) {
    checkExpected(graphqlFile) {
      val pragmas = it.pragmas()
      it.parseAsGQLDocument(pragmas.toParserOptions())
          .getOrThrow()
          .mergeExtensions(pragmas.toMergerOptions())
          .serialize()
    }
  }

  class ParametersProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<File> {
      return findFiles("test-fixtures/merger")
          .filter { it.name.endsWith(".graphql") && !it.name.endsWith(".expected.graphql") }
    }
  }
}