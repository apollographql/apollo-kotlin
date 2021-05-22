package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.encodeToGraphQLSingleQuoted
import com.google.common.truth.Truth
import org.junit.Test

class StringEncodingTest {
  @Test
  fun `single_quotes quotes are escaped`() {
    Truth.assertThat("a \"quote\"".encodeToGraphQLSingleQuoted()).isEqualTo("a \\\"quote\\\"")
  }

  @Test
  fun `single_quotes newlines are escaped`() {
    Truth.assertThat("""
      a
      line""".trimIndent().encodeToGraphQLSingleQuoted()).isEqualTo("a\\nline")
  }
}
