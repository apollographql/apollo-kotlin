package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLSelection

internal interface FieldMerger {
  fun merge(fields: List<FieldWithParent>): List<MergedField>
}

internal data class FieldWithParent(val gqlField: GQLField, val parentType: String)

internal data class MergedField(
    val info: IrFieldInfo,
    val condition: BooleanExpression<BVariable>,
    val selections: List<GQLSelection>,
    /**
     * The name of the rawType, without the NotNull/List decorations
     * When selections is not empty, this is the type condition for these selections
     *
     * We cannot rely on [info.type] to get it because [info.type] represents a Kotlin model and lost this already
     */
    val rawTypeName: String,
)

internal fun collectFields(
    allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    selections: List<GQLSelection>,
    typenameInScope: String,
    typeSet: TypeSet,
): List<FieldWithParent> {
  return selections.flatMap {
    when (it) {
      is GQLField -> listOf(FieldWithParent(it, typenameInScope))
      is GQLInlineFragment -> {
        if (typeSet.contains(it.typeCondition.name)) {
          collectFields(allFragmentDefinitions, it.selectionSet.selections, it.typeCondition.name, typeSet)
        } else {
          emptyList()
        }
      }
      is GQLFragmentSpread -> {
        val fragment = allFragmentDefinitions[it.name]!!
        if (typeSet.contains(fragment.typeCondition.name)) {
          collectFields(allFragmentDefinitions, fragment.selectionSet.selections, fragment.typeCondition.name, typeSet)
        } else {
          emptyList()
        }
      }
    }
  }
}