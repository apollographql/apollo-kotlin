package com.apollographql.apollo3.graphql.ast.test.introspection

import com.apollographql.apollo3.ast.SourceAwareException
import com.apollographql.apollo3.ast.introspection.toSchemaGQLDocument
import com.apollographql.apollo3.ast.validateAsSchema
import org.junit.Test
import java.io.File

class IntrospectionTest {
  @Test
  fun parseSchema() {
    try {
      File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/introspection/duplicate.json").toSchemaGQLDocument().validateAsSchema().valueAssertNoErrors()
    } catch (e: SourceAwareException) {
      assert(e.message!!.contains("is defined multiple times"))
    }
  }
}
