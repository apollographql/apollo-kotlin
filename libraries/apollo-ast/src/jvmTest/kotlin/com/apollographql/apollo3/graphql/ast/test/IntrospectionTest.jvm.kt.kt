package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.introspection.toGQLDocument
import com.apollographql.apollo3.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo3.ast.introspection.toJson
import com.apollographql.apollo3.ast.toFullSchemaGQLDocument
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.graphql.ast.test.ParserTest.Companion.checkExpected
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class IntrospectionTest {
  private fun canReadIntrospectionResults(name: String) {
    File("${CWD}/test-fixtures/introspection/$name.json")
        .toIntrospectionSchema()
        .toGQLDocument()
        .validateAsSchema()
        .getOrThrow()
  }

  @Test
  fun canReadJuly2015IntrospectionResults() {
    canReadIntrospectionResults("july2015")
  }

  @Test
  fun canReadJune2018IntrospectionResults() {
    canReadIntrospectionResults("june2018")
  }

  @Test
  fun canReadOctober2021IntrospectionResults() {
    canReadIntrospectionResults("october2021")
  }

  @Test
  fun canReadDraftIntrospectionResults() {
    canReadIntrospectionResults("draft")
  }

  @Test
  fun canWriteConfettiSchema() {
    checkExpected(File("${CWD}/test-fixtures/introspection/confetti.graphqls")) {
      it.toGQLDocument()
          .toFullSchemaGQLDocument()
          .toIntrospectionSchema()
          .toJson()
    }
  }
}