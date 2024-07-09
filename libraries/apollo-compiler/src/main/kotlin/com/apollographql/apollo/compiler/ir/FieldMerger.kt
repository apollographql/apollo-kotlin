package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.CatchTo
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection

internal interface FieldMerger {
  fun merge(fields: List<FieldWithParent>, defaultCatchTo: CatchTo?): List<MergedField>
}

internal data class FieldWithParent(val gqlField: GQLField, val parentType: String)

internal data class MergedField(
    val info: IrFieldInfo,
    val condition: BooleanExpression<BVariable>,
    val selections: List<GQLSelection>,
    /**
     * The name of the rawType, without the NotNull/List decorations
     * When selections are not empty, this is the type condition for these selections
     *
     * We cannot rely on [info.type] to get it because [info.type] represents a Kotlin model and lost this already
     */
    val rawTypeName: String,
)

internal fun collectFields(
    allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    selections: List<GQLSelection>,
    parentTypeDefinition: String,
    typeSet: TypeSet,
): List<FieldWithParent> {
  return selections.flatMap {
    when (it) {
      is GQLField -> listOf(FieldWithParent(it, parentTypeDefinition))
      is GQLInlineFragment -> {
        val tc = it.typeCondition?.name ?: parentTypeDefinition
        if (typeSet.contains(tc)) {
          collectFields(allFragmentDefinitions, it.selections, tc, typeSet)
        } else {
          emptyList()
        }
      }
      is GQLFragmentSpread -> {
        val fragment = allFragmentDefinitions[it.name]!!
        if (typeSet.contains(fragment.typeCondition.name)) {
          collectFields(allFragmentDefinitions, fragment.selections, fragment.typeCondition.name, typeSet)
        } else {
          emptyList()
        }
      }
    }
  }
}
