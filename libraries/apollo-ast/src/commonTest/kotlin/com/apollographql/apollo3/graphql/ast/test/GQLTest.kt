package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.GQLListNullability
import com.apollographql.apollo3.ast.GQLNullDesignator
import com.apollographql.apollo3.ast.parseAsGQLType
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.withNullability
import kotlin.test.Test
import kotlin.test.assertEquals

class GQLTest {
  @Test
  fun type() {
    val newType = "[String!]!".parseAsGQLType().getOrThrow().withNullability(GQLListNullability(selfNullability = null, itemNullability = GQLNullDesignator()))

    assertEquals("[String]!", newType.pretty())
  }
}