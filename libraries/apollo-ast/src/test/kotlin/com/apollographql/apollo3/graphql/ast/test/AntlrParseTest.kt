package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.internal.buffer
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.parseAsGQLValue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class AntlrParseTest {
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
      check(e.message?.contains("Extra token at end of file") == true)
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
}
