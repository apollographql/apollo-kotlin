package com.apollographql.apollo3.compiler.unified.ir

import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.frontend.GQLField
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo3.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo3.compiler.frontend.GQLSelection
import com.apollographql.apollo3.compiler.frontend.Schema

/**
 * For a list of selections collect all the typeConditions.
 * Then for each combination of typeConditions, collect all the fields recursively.
 *
 * While doing so, record all the used fragments and used types
 */
class IrRootFieldBuilder(
    private val schema: Schema,
    private val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldMerger: FieldMerger,
) : RootFieldBuilder {

  val collectedFragments = mutableSetOf<String>()

  override fun build(
      selections: List<GQLSelection>,
      rawTypeName: String,
  ): IrField {
    val info = IrFieldInfo(
        name = "data",
        alias = null,
        description = null,
        deprecationReason = null,
        arguments = emptyList(),
        type = IrModelType(IrUnknownModelId),
        rawTypeName = rawTypeName
    )

    return buildField(
        info = info,
        selections = selections,
        condition = BooleanExpression.True,
    )
  }

  private fun List<GQLSelection>.collectUserTypeSets(currentTypeSet: TypeSet): List<TypeSet> {
    return listOf(currentTypeSet) + flatMap {
      when (it) {
        is GQLField -> emptyList()
        is GQLInlineFragment -> it.selectionSet.selections.collectUserTypeSets(currentTypeSet + it.typeCondition.name)
        is GQLFragmentSpread -> {
          collectedFragments.add(it.name)
          val fragmentDefinition = allGQLFragmentDefinitions[it.name]!!
          fragmentDefinition.selectionSet.selections.collectUserTypeSets(currentTypeSet + fragmentDefinition.typeCondition.name)
        }
      }
    }
  }

  private fun List<GQLSelection>.collectFragments(): Set<String> {
    return flatMap {
      when (it) {
        is GQLField -> emptySet()
        is GQLInlineFragment -> {
          it.selectionSet.selections.collectFragments()
        }
        // We do not recurse here as inheriting the first namedFragment will
        // inherit nested ones as well
        is GQLFragmentSpread -> return setOf(it.name)
      }
    }.toSet()
  }

  private fun buildField(
      info: IrFieldInfo,
      condition: BooleanExpression,
      selections: List<GQLSelection>,
  ): IrField {
    if (selections.isEmpty()) {
      return IrField(
          info = info,
          condition = condition,
          fieldSets = emptyList(),
          fragments = emptySet()
      )
    }

    val rawTypeName = info.rawTypeName
    val userTypeSets = selections.collectUserTypeSets(setOf(rawTypeName)).distinct().toSet()
    val typeConditions = userTypeSets.union()

    val shapeTypeSetToPossibleTypes = computeShapes(schema, rawTypeName, typeConditions)
    val shapeTypeSets = shapeTypeSetToPossibleTypes.keys

    val fieldSets = (userTypeSets + shapeTypeSets).map { typeSet ->
      buildFieldSet(
          selections = selections,
          rawTypeName = rawTypeName,
          typeSet = typeSet,
          possibleTypes = shapeTypeSetToPossibleTypes[typeSet] ?: emptySet(),
      )
    }

    return IrField(
        info = info,
        condition = condition,
        fieldSets = fieldSets,
        fragments = selections.collectFragments()
    )
  }

  /**
   */
  private fun collectFieldsInternal(
      selections: List<GQLSelection>,
      typeCondition: String,
      typeSet: TypeSet,
  ): List<FieldWithParent> {
    return selections.flatMap {
      when (it) {
        is GQLField -> listOf(FieldWithParent(it, typeCondition))
        is GQLInlineFragment -> {
          if (typeSet.contains(it.typeCondition.name)) {
            collectFieldsInternal(it.selectionSet.selections, it.typeCondition.name, typeSet)
          } else {
            emptyList()
          }
        }
        is GQLFragmentSpread -> {
          val fragment = allGQLFragmentDefinitions[it.name]!!
          if (typeSet.contains(fragment.typeCondition.name)) {
            collectFieldsInternal(fragment.selectionSet.selections, fragment.typeCondition.name, typeSet)
          } else {
            emptyList()
          }
        }
      }
    }
  }

  private fun buildFieldSet(
      selections: List<GQLSelection>,
      rawTypeName: String,
      typeSet: TypeSet,
      possibleTypes: PossibleTypes,
  ): IrFieldSet {
    val fields = collectFieldsInternal(
        selections = selections,
        typeCondition = rawTypeName,
        typeSet = typeSet,
    ).let {
      fieldMerger.merge(it)
    }.map { mergedField ->
      buildField(
          info = mergedField.info,
          condition = mergedField.condition,
          selections = mergedField.selections,
      )
    }

    return IrFieldSet(
        typeSet = typeSet,
        possibleTypes = possibleTypes,
        fields = fields,
    )
  }
}
