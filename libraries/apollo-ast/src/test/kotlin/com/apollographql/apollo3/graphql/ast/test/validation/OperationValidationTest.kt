package com.apollographql.apollo3.graphql.ast.test.validation

import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.introspection.toSchema
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.validateAsExecutable
import okio.buffer
import okio.source
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class OperationValidationTest {
  @Test
  fun testAddRequiredFields() {
    val schema = File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/validation/inputTypeDeprecatedField.graphqls").toSchema()
    val operations = File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/validation/inputTypeDeprecatedField.graphql")
        .source()
        .buffer()
        .parseAsGQLDocument()
        .getOrThrow()
    val operationIssues = operations.validateAsExecutable(schema).issues
    assertEquals(1, operationIssues.size)
    assertEquals("Use of deprecated input field `deprecatedParameter`", operationIssues[0].message)
    assertEquals(SourceLocation(12, 41, -1, -1, null).pretty(), operationIssues[0].sourceLocation?.pretty())
  }
}
