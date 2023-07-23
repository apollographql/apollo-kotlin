package com.apollographql.apollo3.graphql.ast.test

import com.apollographql.apollo3.ast.GQLListNullability
import com.apollographql.apollo3.ast.GQLOptional
import com.apollographql.apollo3.ast.parseAsGQLType
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.withCcn
import kotlin.test.Test
import kotlin.test.assertEquals

class GQLTest {
  @Test
  fun type() {
    val newType = "[String!]!".parseAsGQLType().getOrThrow().withCcn(GQLListNullability(selfNullability = null, itemNullability = GQLOptional()))

    assertEquals("[String]!", newType.pretty())
  }
}