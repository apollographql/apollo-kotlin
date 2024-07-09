package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.validateAsExecutable
import com.apollographql.apollo.compiler.TestUtils.checkExpected
import com.apollographql.apollo.compiler.TestUtils.findSchema
import com.apollographql.apollo.compiler.TestUtils.serialize
import okio.buffer
import okio.source
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FieldsOnDisjointTypesMustMergeTest {
  private val graphQLFile = File("src/test/validation/operation/fields_on_disjoint_types_must_merge/different_shapes.graphql")

  @Test
  fun testEnableFieldsOnDisjointTypesMustMerge() = checkExpected(graphQLFile) { schema ->
    val parseResult = graphQLFile
        .source()
        .buffer()
        .parseAsGQLDocument(graphQLFile.name) // strip parts of the path
    val issues = parseResult.getOrThrow().validateAsExecutable(schema = schema!!).issues
    issues.serialize()
  }
}
