package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLField
import com.apollographql.apollo3.compiler.frontend.GQLSelection

interface FieldMerger {

  fun merge(fields: List<FieldWithParent>): List<MergedField>
}

data class FieldWithParent(val gqlField: GQLField, val parentType: String)
data class MergedField(
    val info: IrFieldInfo,
    val condition: BooleanExpression,
    val selections: List<GQLSelection>,
    /**
     * The name of the rawType, without the NotNull/List decorations
     * When selections is not empty, this is the type condition for these selections
     */
    val rawTypeName: String,
)