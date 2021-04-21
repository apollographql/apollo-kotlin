package com.apollographql.apollo3.compiler.unified.ir

import com.apollographql.apollo3.compiler.frontend.GQLSelection

interface RootFieldBuilder {
  fun build(
      selections: List<GQLSelection>,
      rawTypeName: String,
  ): IrField
}