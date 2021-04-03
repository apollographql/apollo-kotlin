package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLField
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo3.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo3.compiler.frontend.GQLSelection
import com.apollographql.apollo3.compiler.frontend.Schema

/**
 * @param fieldCollector a [FieldCollector] that handles the heavy lifting of collecting fields, remember their types, etc...
 */
class AsClassesFieldSetBuilder(
    private val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldCollector: FieldCollector,
) {
  private var cachedFragmentsFields = mutableMapOf<String, IrField>()

  fun buildOperationField(
      name: String,
      selections: List<GQLSelection>,
      type: IrCompoundType,
  ): IrField {
    val path = ModelPath(root = ModelPath.Root.Operation(name))

    return buildRootField(
        name = "data",
        selections = selections,
        type = type,
        path = path
    )
  }

  private fun getOrBuildFragmentField(
      selections: List<GQLSelection>,
      type: IrCompoundType,
      name: String,
  ): IrField {
    val path = ModelPath(root = ModelPath.Root.FragmentModel(name))

    return cachedFragmentsFields.getOrPut(name) {
      buildRootField(
          name = name,
          selections = selections,
          type = type,
          path = path
      )
    }
  }

  private fun buildRootField(
      name: String,
      selections: List<GQLSelection>,
      type: IrCompoundType,
      path: ModelPath,
  ): IrField {
    val info = IrFieldInfo(
        name = name,
        alias = null,
        description = null,
        deprecationReason = null,
        arguments = emptyList(),
        type = type,
    )

    return buildField(
        info = info,
        selections = selections,
        path = path,
        condition = BooleanExpression.True,
    )
  }

  private fun buildField(
      info: IrFieldInfo,
      condition: BooleanExpression,
      selections: List<GQLSelection>,
      path: ModelPath,
  ): IrField {
    val fieldSets: List<IrFieldSet>
    val interfaceFieldSets: List<IrFieldSet>
    val implementationFieldSets: List<IrFieldSet>
    val fragmentAccessors = mutableListOf<IrFragmentAccessor>()


    if (selections.isNotEmpty()) {
      val fieldType = (info.type.leafType() as? IrCompoundType)?.name ?: "Got selections on a non-compound type for ${info.responseName}"

      val modelName = info.responseName.capitalize()

      val selfFields = fieldCollector.collectFields(
          selections = selections,
          typeCondition = fieldType,
          typeSet = setOf(fieldType),
          collectInlineFragments = false,
          collectNamedFragments = false
      ) { childInfo, childCondition, childSelections ->
        buildField(childInfo, childCondition, childSelections, path + modelName)
      }

      val inlineFields = selections.filterIsInstance<GQLInlineFragment>()
          .groupBy { it.typeCondition.name }
          .map {
            val syntheticName = "as${it.key.capitalize()}"
            val syntheticModelName = syntheticName.capitalize()
            val childInfo = IrFieldInfo(
                alias = null,
                name = syntheticName,
                arguments = emptyList(),
                description = "Synthetic field for inline fragment",
                deprecationReason = null,
                type = IrCompoundType(syntheticModelName)
            )

            buildField(
                info = childInfo,
                condition = BooleanExpression.Or(it.value.map { it.directives.toBooleanExpression() }.toSet()),
                selections = selections + it.value,
                path = path + syntheticModelName,
            )
          }

      val fragmentFields = selections.filterIsInstance<GQLFragmentSpread>()
          .groupBy { it.name }
          .map {
            val fragmentDefinition = allGQLFragmentDefinitions[it.key]!!
            getOrBuildFragmentField(
                selections = fragmentDefinition.selectionSet.selections,
                type = IrCompoundType(fragmentDefinition.typeCondition.name),
                name = fragmentDefinition.name
            )
          }
      fieldSets = listOf(
          IrFieldSet(
              path = path,
              modelName = modelName,
              typeSet = setOf(fieldType),
              fields = selfFields + inlineFields + fragmentFields,
              implements = emptySet(),
              possibleTypes = emptySet(),
              syntheticFields = emptyList()
          )
      )
      interfaceFieldSets = emptyList()
      implementationFieldSets = fieldSets
    } else {
      fieldSets = emptyList()
      interfaceFieldSets = emptyList()
      implementationFieldSets = emptyList()
    }

    val typeFieldSet = implementationFieldSets.firstOrNull()

    return IrField(
        info = info,
        condition = condition,
        override = false,

        typeFieldSet = typeFieldSet,
        fieldSets = fieldSets,
        interfaces = interfaceFieldSets,
        implementations = implementationFieldSets,
        fragmentAccessors = fragmentAccessors.distinctBy { it.name }
    )
  }
}
