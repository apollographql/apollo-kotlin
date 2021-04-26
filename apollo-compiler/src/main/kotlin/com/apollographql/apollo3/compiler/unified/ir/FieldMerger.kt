package com.apollographql.apollo3.compiler.unified.ir

import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.graphql.ast.GQLField
import com.apollographql.apollo3.graphql.ast.GQLSelection

interface FieldMerger {
  fun merge(fields: List<FieldWithParent>): List<MergedField>
}

data class FieldWithParent(val gqlField: GQLField, val parentType: String)
data class MergedField(
    val info: IrFieldInfo,
    val condition: BooleanExpression,
    val selections: List<GQLSelection>,
)