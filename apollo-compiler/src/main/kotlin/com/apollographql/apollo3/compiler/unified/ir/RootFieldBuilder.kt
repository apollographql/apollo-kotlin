package com.apollographql.apollo3.compiler.unified.ir

import com.apollographql.apollo3.graphql.ast.GQLSelection

interface RootFieldBuilder {
  fun build(
      selections: List<GQLSelection>,
      rawTypeName: String,
  ): IrField
}