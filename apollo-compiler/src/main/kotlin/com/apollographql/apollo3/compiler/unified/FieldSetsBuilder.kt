package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLSelection

interface FieldSetsBuilder {
  fun buildOperationField(
      name: String,
      selections: List<GQLSelection>,
      type: IrCompoundType,
  ): IrField
}