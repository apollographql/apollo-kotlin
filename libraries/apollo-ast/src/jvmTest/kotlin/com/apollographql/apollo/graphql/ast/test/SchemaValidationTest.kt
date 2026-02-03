package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.validateAsSchema
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class SchemaValidationTest {
  @Test
  fun testValidation(@TestParameter(valuesProvider = ParametersProvider::class) graphqlsFile: File) {

    checkExpected(graphqlsFile) {
      val pragmas = graphqlsFile.pragmas()
      val parseResult = graphqlsFile.toGQLDocument().validateAsSchema(
          pragmas.toSchemaValidationOptions()
      )

      parseResult.issues.serialize()
    }
  }

  class ParametersProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<File> {
      return findFiles("test-fixtures/validation/schema")
          .filter { it.name.endsWith(".graphqls") }
    }
  }
}
