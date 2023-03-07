package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.validateAsExecutable
import com.apollographql.apollo3.compiler.TestUtils.checkExpected
import com.apollographql.apollo3.compiler.TestUtils.findSchema
import com.apollographql.apollo3.compiler.TestUtils.serialize
import okio.buffer
import okio.source
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FieldsOnDisjointTypesMustMergeTest {
  private val graphQLFile = File("src/test/validation/operation/fields_on_disjoint_types_must_merge/different_shapes.graphql")

  @Test
  fun testEnableFieldsOnDisjointTypesMustMerge() = checkExpected(graphQLFile) { schema ->
    val parseResult = graphQLFile.source().buffer().parseAsGQLDocument(filePath = graphQLFile.name)
    val issues = parseResult.getOrThrow().validateAsExecutable(schema = schema!!, fieldsOnDisjointTypesMustMerge = true).issues
    issues.serialize()
  }

  @Test
  fun testDisableFieldsOnDisjointTypesMustMerge() {
    val schema = findSchema(graphQLFile.parentFile)!!
    val parseResult = graphQLFile.source().buffer().parseAsGQLDocument(filePath = graphQLFile.name)

    val issues = parseResult.getOrThrow().validateAsExecutable(schema = schema, fieldsOnDisjointTypesMustMerge = false).issues

    assertTrue(issues.isEmpty())
  }
}
