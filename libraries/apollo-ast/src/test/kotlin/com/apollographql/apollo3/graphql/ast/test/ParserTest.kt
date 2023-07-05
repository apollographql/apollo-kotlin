package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFieldDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.internal.buffer
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.parseAsGQLValue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ParserTest {
  @Test
  fun extraTokensAtEndOfFileAreDetected() {
    try {
      """
      # comment before
      query Test { field }
      ab bc
      """.trimIndent()
          .buffer()
          .parseAsGQLDocument()
          .getOrThrow()
      fail("An exception was expected")
    } catch (e: Exception) {
      check(e.message?.contains("Unexpected token: 'name: ab'") == true)
    }
  }

  @Test
  fun extraCommentsAtEndOfFileAreOk() {
    """
      # comment before
      query Test { field }
      # comment after
      """.trimIndent()
        .buffer()
        .parseAsGQLDocument()
        .getOrThrow()
  }

  @Test
  fun blockString() {
    val value = "\"\"\" \\\"\"\" \"\"\"".buffer().parseAsGQLValue().getOrThrow()

    assertEquals(" \"\"\" ", (value as GQLStringValue).value)
  }

  @Test
  fun blockStringIndentationIsRemoved() {
    val value = ("\"\"\"\n" +
        "  first line\n" +
        "   \n" +
        "  second line\n" +
        "   \n" +
        "\"\"\"").buffer().parseAsGQLValue().getOrThrow()

    assertEquals("first line\n \nsecond line", (value as GQLStringValue).value)
  }

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
        .buffer()
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
          assertEquals(4, line)
          assertEquals(6, endLine)
          assertEquals(3, column)
          assertEquals(5, endColumn)
        }
  }

  @Test
  fun endLineAndColumn2() {
    """
      type Something { 
        fieldA: String
      }
    """.trimIndent()
        .buffer()
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
          assertEquals(2, line)
          assertEquals(2, endLine)
          assertEquals(3, column)
          assertEquals(16, endColumn)
        }
  }
}
