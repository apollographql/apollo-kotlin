package test.validation

import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import com.apollographql.apollo.ast.validateAsExecutable
import com.apollographql.apollo.graphql.ast.test.CWD
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OperationValidationTest {
  @Test
  fun deprecatedInputField() {
    val schema = "${CWD}/src/filesystemTest/kotlin/test/validation/inputTypeDeprecatedField.graphqls"
        .toPath()
        .toGQLDocument()
        .toSchema()

    val operations = "${CWD}/src/filesystemTest/kotlin/test/validation/inputTypeDeprecatedField.graphql"
        .toPath()
        .toGQLDocument()

    val operationIssues = operations.validateAsExecutable(schema).issues
    assertEquals(1, operationIssues.size)
    assertEquals("Use of deprecated input field `deprecatedParameter`", operationIssues[0].message)
    operationIssues[0].sourceLocation.apply {
      assertNotNull(this)
      assertEquals(247, start)
      assertEquals(269, end)
      assertEquals(12, line)
      assertEquals(41, column)
    }
  }
}
