package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.GQLListNullability
import com.apollographql.apollo3.ast.GQLNullDesignator
import com.apollographql.apollo3.ast.parseAsGQLNullability
import com.apollographql.apollo3.ast.parseAsGQLType
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.withNullability
import kotlin.test.Test
import kotlin.test.assertEquals

class CcnTest {
  @Test
  fun type() {
    val newType = "[String!]!".parseAsGQLType().getOrThrow().withNullability(GQLListNullability(selfNullability = null, itemNullability = GQLNullDesignator()))

    assertEquals("[String]!", newType.pretty())
  }

  @Test
  fun nullability() {
    try {
      "[[[String]]]".parseAsGQLType().getOrThrow().withNullability("[[[[!]]]]".parseAsGQLNullability().getOrThrow())
      error("an exception was expected")
    } catch (e: Exception) {
      assertEquals(true, e.message?.contains("the nullability list dimension exceeds the one of the field type"))
    }

  }
}