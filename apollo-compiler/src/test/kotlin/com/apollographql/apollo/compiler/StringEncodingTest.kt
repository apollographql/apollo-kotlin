package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.frontend.GraphQLString
import com.google.common.truth.Truth
import org.junit.Test

class StringEncodingTest {
  @Test
  fun `single_quotes quotes are escaped`() {
    Truth.assertThat(GraphQLString.encodeSingleQuoted("a \"quote\"")).isEqualTo("a \\\"quote\\\"")
  }

  @Test
  fun `single_quotes newlines are escaped`() {
    Truth.assertThat(GraphQLString.encodeSingleQuoted("""
      a
      line""".trimIndent())).isEqualTo("a\\nline")
  }
}
