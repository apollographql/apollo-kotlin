package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.internal.buffer
import com.apollographql.apollo3.ast.parseAsGQLDocument
import org.junit.Test
import kotlin.test.fail

@OptIn(ApolloExperimental::class)
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
          .valueAssertNoErrors()
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
          .valueAssertNoErrors()
  }
}