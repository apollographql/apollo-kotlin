package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.backend.codegen.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.frontend.GQLField
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo3.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo3.compiler.frontend.GQLSelection
import com.apollographql.apollo3.compiler.frontend.responseName

/**
 * A [FieldSetsBuilder] that generates fragments as classes. To do so, it maps the query and generates synthetic fields for
 * inline and named fragments.
 * - named fragments will generate reusable data classes
 * - inline fragments will also generate data classes with the difference that these data classes carry over the fields from the enclosing type
 *
 * No attempt is made to simplify trivial inline fragments
 */
class AsClassesFieldSetBuilder(
    private val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldMerger: FieldMerger,
) : FieldSetsBuilder {
  private var cachedFragmentsFields = mutableMapOf<String, IrField>()

  override fun buildOperationField(
      name: String,
      selections: List<GQLSelection>,
      type: IrCompoundType,
  ): IrField {
    val path = ModelPath(root = ModelPath.Root.Operation(name))

    return buildRootField(
        name = "data",
        selections = selections,
        type = type,
        path = path,
    )
  }

  fun getOrBuildFragmentField(
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
        fieldType = type.name
    )
  }

  private fun buildField(
      info: IrFieldInfo,
      condition: BooleanExpression,
      selections: List<GQLSelection>,
      fieldType: String?,
      path: ModelPath,
  ): IrField {
    return buildField(
        info = info,
        condition = condition,
        fields = selections.filterIsInstance<GQLField>().map {
          check(fieldType != null) {
            "No field type for field with selections: ${it.responseName()}"
          }
          FieldWithParent(it, fieldType)
        },
        inlineFragments = selections.filterIsInstance<GQLInlineFragment>(),
        namedFragments = selections.filterIsInstance<GQLFragmentSpread>(),
        path = path
    )
  }

  private fun buildField(
      info: IrFieldInfo,
      condition: BooleanExpression,
      fields: List<FieldWithParent>,
      inlineFragments: List<GQLInlineFragment>,
      namedFragments: List<GQLFragmentSpread>,
      path: ModelPath,
  ): IrField {
    val fieldSets: List<IrFieldSet>
    val interfaceFieldSets: List<IrFieldSet>
    val implementationFieldSets: List<IrFieldSet>
    val fragmentAccessors = mutableListOf<IrFragmentAccessor>()

    val modelName = capitalizeFirstLetter(info.responseName)

    val selfFields = fieldMerger.merge(fields).map { mergedField ->
      buildField(
          info = mergedField.info,
          condition = mergedField.condition,
          selections = mergedField.selections,
          fieldType = mergedField.rawTypeName,
          path = path + modelName
      )
    }

    val inlineFields = inlineFragments
        .groupBy { it.typeCondition.name }
        .map {
          val typeCondition = it.key
          val syntheticName = "as${typeCondition.capitalize()}"

          val childInfo = IrFieldInfo(
              alias = null,
              name = syntheticName,
              arguments = emptyList(),
              description = "Synthetic field for inline fragment",
              deprecationReason = null,
              type = IrCompoundType("unused")
          )

          val inlineSelections = it.value.flatMap { it.selectionSet.selections }

          buildField(
              info = childInfo,
              condition = BooleanExpression.Or(it.value.map { it.directives.toBooleanExpression() }.toSet()),
              fields = fields + inlineSelections.filterIsInstance<GQLField>().map { FieldWithParent(it, typeCondition) },
              inlineFragments = inlineSelections.filterIsInstance<GQLInlineFragment>(),
              namedFragments = namedFragments + inlineSelections.filterIsInstance<GQLFragmentSpread>(),
              path = path + modelName,
          )
        }

    val fragmentFields = namedFragments
        .groupBy { it.name }
        .map {
          val fragmentDefinition = allGQLFragmentDefinitions[it.key]!!
          val fragmentField = getOrBuildFragmentField(
              selections = fragmentDefinition.selectionSet.selections,
              type = IrCompoundType(fragmentDefinition.typeCondition.name),
              name = fragmentDefinition.name
          )

          val syntheticName = it.key.decapitalize()
          val childInfo = IrFieldInfo(
              alias = null,
              name = syntheticName,
              arguments = emptyList(),
              description = "Synthetic field for inline fragment",
              deprecationReason = null,
              type = IrCompoundType("unused")
          )

          IrField(
              info = childInfo,
              typeFieldSet = fragmentField.typeFieldSet,
              condition = BooleanExpression.Or(it.value.map { it.directives.toBooleanExpression() }.toSet()),
              override = false,
              fieldSets = emptyList(),
              interfaces = emptyList(),
              implementations = emptyList(),
              fragmentAccessors = emptyList()
          )
        }

    val allFields = selfFields + inlineFields + fragmentFields
    if (allFields.isNotEmpty()) {
      fieldSets = listOf(
          IrFieldSet(
              path = path,
              modelName = modelName,
              // Pass some identifiable string in case someone bumps into this
              typeSet = setOf("fieldSets for fragments as classes do not match a single typeSet"),
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
