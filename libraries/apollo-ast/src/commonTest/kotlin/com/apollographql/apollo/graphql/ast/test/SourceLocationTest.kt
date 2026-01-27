package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.parseAsGQLDocument
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceLocationTest {
  private inline fun <reified T> Any.cast() = this as T

  @Test
  fun endLineAndColumn() {
    """
      # comment before
query Test {
  # comment here
  field(
    first: 100
    )
}
      """.trimIndent()
        .parseAsGQLDocument()
        .getOrThrow()
        .definitions
        .first()
        .cast<GQLOperationDefinition>()
        .selections
        .first()
        .cast<GQLField>()
        .sourceLocation!!
        .apply {
          assertEquals(55, start)
          assertEquals(82, end)
          assertEquals(4, line)
          assertEquals(3, column)
        }
  }

  @Test
  fun endLineAndColumn2() {
    """
      type Something { 
        fieldA: String
      }
    """.trimIndent()
        .parseAsGQLDocument()
        .getOrThrow()
        .definitions
        .first()
        .cast<GQLObjectTypeDefinition>()
        .fields
        .first()
        .cast<GQLFieldDefinition>()
        .sourceLocation!!
        .apply {
          assertEquals(20, start)
          assertEquals(34, end)
          assertEquals(2, line)
          assertEquals(3, column)
        }
  }
}
