package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.internal.buffer
import com.apollographql.apollo3.ast.toSchema
import org.junit.Test

@OptIn(ApolloExperimental::class)
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

    schemaString.buffer().toSchema()
  }
}