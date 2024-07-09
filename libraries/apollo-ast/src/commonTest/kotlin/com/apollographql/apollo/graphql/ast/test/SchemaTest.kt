package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import kotlin.test.Test

class SchemaTest {
  @Test
  fun schemaMayContainBuiltinDirectives() {
    val schemaString = """
      "Directs the executor to include this field or fragment only when the `if` argument is true"
      directive @include(
          "Included when true."
          if: Boolean!
      ) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT
      
      type Query {
        foo: Int
      }
    """.trimIndent()

    schemaString.toGQLDocument().toSchema()
  }
}
