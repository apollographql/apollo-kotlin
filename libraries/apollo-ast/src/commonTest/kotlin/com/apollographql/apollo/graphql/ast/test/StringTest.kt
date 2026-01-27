package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.encodeToGraphQLSingleQuoted
import com.apollographql.apollo.ast.parseAsGQLValue
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

  @Test
  fun blockString() {
    val value = "\"\"\" \\\"\"\" \"\"\"".parseAsGQLValue().getOrThrow()

    assertEquals(" \"\"\" ", (value as GQLStringValue).value)
  }

  @Test
  fun blockStringIndentationIsRemoved() {
    val value = ("\"\"\"\n" +
        "  first line\n" +
        "   \n" +
        "  second line\n" +
        "   \n" +
        "\"\"\"").parseAsGQLValue().getOrThrow()

    assertEquals("first line\n \nsecond line", (value as GQLStringValue).value)
  }
}