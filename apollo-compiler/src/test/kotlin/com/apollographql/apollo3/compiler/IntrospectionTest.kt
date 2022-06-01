package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.SourceAwareException
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.ast.introspection.toSchemaGQLDocument
import org.junit.Test
import java.io.File

class IntrospectionTest {
  @Test
  fun parseSchema() {
    try {
      File("src/test/introspection/duplicate.json").toSchemaGQLDocument().validateAsSchema().valueAssertNoErrors()
    } catch (e: SourceAwareException) {
      assert(e.message!!.contains("is defined multiple times"))
    }
  }
}
