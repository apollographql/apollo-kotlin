package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLSelection

interface FieldCollector {
  fun collectFields(
      selections: List<GQLSelection>,
      typeCondition: String,
      typeSet: TypeSet,
      collectInlineFragments: Boolean,
      collectNamedFragments: Boolean,
      block: (IrFieldInfo, BooleanExpression, List<GQLSelection>) -> IrField
  ): List<IrField>
}