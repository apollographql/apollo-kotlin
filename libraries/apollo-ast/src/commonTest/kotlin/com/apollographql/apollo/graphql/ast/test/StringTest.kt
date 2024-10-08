package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.encodeToGraphQLSingleQuoted
import kotlin.test.Test
import kotlin.test.assertEquals

class StringTest {
  @Test
  fun slashesAreNotEscapedWhenWriting() {
    assertEquals("rest/api/3/search", "rest/api/3/search".encodeToGraphQLSingleQuoted())
  }

  @Test
  fun controlCodesAreEscaped() {
    assertEquals("\\n\\u0003", "\n\u0003".encodeToGraphQLSingleQuoted())
  }
}