package com.apollographql.apollo3.graphql.ast.test.validation

import com.apollographql.apollo3.ast.HOST_FILESYSTEM
import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.introspection.toSchema
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.validateAsExecutable
import com.apollographql.apollo3.graphql.ast.test.CWD
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class OperationValidationTest {
  @Test
  fun testAddRequiredFields() {
    val schema = "${CWD}/src/commonTest/kotlin/com/apollographql/apollo3/graphql/ast/test/validation/inputTypeDeprecatedField.graphqls".toPath().toSchema()
    val operations = "${CWD}/src/commonTest/kotlin/com/apollographql/apollo3/graphql/ast/test/validation/inputTypeDeprecatedField.graphql"
        .toPath()
        .let { HOST_FILESYSTEM.source(it) }
        .buffer()
        .parseAsGQLDocument(null) // Use filePath = null to remove the absolute path above
        .getOrThrow()
    val operationIssues = operations.validateAsExecutable(schema).issues
    assertEquals(1, operationIssues.size)
    assertEquals("Use of deprecated input field `deprecatedParameter`", operationIssues[0].message)
    assertEquals(
        SourceLocation(
            start = 0,
            end = 1,
            line = 12,
            column = 41,
            null
        ).pretty(),
        operationIssues[0].sourceLocation.pretty()
    )
  }
}
