package com.apollographql.apollo3.compiler.unified.ir

import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.possibleTypes

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
        is GQLFragmentSpread -> return@flatMap setOf(it.name)
      }
    }.toSet()
  }

  data class Shape(val typeSet: TypeSet, val schemaPossibleTypes: PossibleTypes, val actualPossibleTypes: PossibleTypes)

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

    val typeConditionToPossibleTypes = userTypeSets.union().map {
      it to schema.typeDefinition(it).possibleTypes(schema)
    }.toMap()

    val shapes = userTypeSets.map {
      Shape(
          it,
          it.map { typeConditionToPossibleTypes[it]!! }.intersection(),
          emptySet()
      )
    }.toMutableList()

    schema.typeDefinitions.values.filterIsInstance<GQLTypeDefinition>().forEach { objectTypeDefinition ->
      val concreteType = objectTypeDefinition.name
      val superShapes = mutableListOf<Shape>()
      shapes.forEachIndexed { index, shape ->
        if (shapes[index].schemaPossibleTypes.contains(concreteType)) {
          superShapes.add(shape)
        }
      }

      if(superShapes.isEmpty()) {
        // This type will not be included in this query
        return@forEach
      }

      /**
       * This type will fall in the bucket that has all the typeConditions. It might be that we have to create a new bucket
       * if two leaf fragments point to the same type
       */
      val bucketTypeSet = superShapes.fold(emptySet<String>()) { acc, shape ->
        acc.union(shape.typeSet)
      }

      val index = shapes.indexOfFirst { it.typeSet == bucketTypeSet }
      if (index < 0) {
        shapes.add(
            Shape(
                bucketTypeSet,
                bucketTypeSet.map { typeConditionToPossibleTypes[it]!! }.intersection(),
                setOf(concreteType)
            )
        )
      } else {
        val existingShape = shapes[index]
        shapes[index] = existingShape.copy(actualPossibleTypes = existingShape.actualPossibleTypes + concreteType)
      }
    }

    val fieldSets = shapes.map { shape ->
      buildFieldSet(
          selections = selections,
          rawTypeName = rawTypeName,
          typeSet = shape.typeSet,
          possibleTypes = shape.actualPossibleTypes,
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
