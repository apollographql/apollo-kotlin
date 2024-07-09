package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.possibleTypes

internal data class Shape(val typeSet: TypeSet, val possibleTypes: PossibleTypes)

/**
 * This function computes the different shapes a given field can take.
 *
 * TODO: refactor to use [buckets]
 * TODO: right now this function keeps user type sets even if some of them are redundant:
 *
 * ```
 * {
 *   cat {
 *     # generates a `interface AnimalCat`
 *     ... on Animal {
 *       species
 *     }
 *   }
 * }
 * ```
 *
 * Simplify to not generate that code
 *
 */
internal fun shapes(
    schema: Schema,
    allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    selections: List<GQLSelection>,
    rawTypename: String,
): List<Shape> {

  val userTypeSets = selections.collectUserTypeSets(allFragmentDefinitions, setOf(rawTypename)).toSet()

  val typeConditionToPossibleTypes = userTypeSets.union().associateWith {
    schema.typeDefinition(it).possibleTypes(schema)
  }

  val shapes = userTypeSets.map { userTypeSet ->
    ShapeInternal(
        userTypeSet,
        userTypeSet.map { typeConditionToPossibleTypes[it]!! }.intersection(),
        emptySet()
    )
  }.toMutableList()

  schema.typeDefinitions.values.forEach { objectTypeDefinition ->
    val concreteType = objectTypeDefinition.name
    val superShapes = mutableListOf<ShapeInternal>()
    shapes.forEachIndexed { index, shape ->
      if (shapes[index].schemaPossibleTypes.contains(concreteType)) {
        superShapes.add(shape)
      }
    }

    if (superShapes.isEmpty()) {
      // This type will not be included in this query
      return@forEach
    }

    /**
     * Take all superShapes of a given type. This type will use a shape matching the union of all the superShapes (most qualified typeset).
     *
     * It might be that the user did not specifically required this particular typeSet. For an example, with following type conditions:
     * [A, B]
     * [A, C]
     *
     * A concrete object matching [A, B, C] needs a new shape.
     */
    val bucketTypeSet = superShapes.fold(emptySet<String>()) { acc, shape ->
      acc.union(shape.typeSet)
    }

    val index = shapes.indexOfFirst { it.typeSet == bucketTypeSet }
    if (index < 0) {
      shapes.add(
          ShapeInternal(
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

  return shapes.map { Shape(it.typeSet, it.actualPossibleTypes) }
}

private fun List<GQLSelection>.collectUserTypeSets(
    allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    currentTypeSet: TypeSet,
): List<TypeSet> {
  return listOf(currentTypeSet) + flatMap {
    when (it) {
      is GQLField -> emptyList()
      is GQLInlineFragment -> it.selections.collectUserTypeSets(
          allGQLFragmentDefinitions,
          currentTypeSet + it.typeCondition?.name.orEmpty()
      )
      is GQLFragmentSpread -> {
        val fragmentDefinition = allGQLFragmentDefinitions[it.name]!!
        fragmentDefinition.selections.collectUserTypeSets(
            allGQLFragmentDefinitions,
            currentTypeSet + fragmentDefinition.typeCondition.name
        )
      }
    }
  }
}

/**
 * A possible response shape
 * @param schemaPossibleTypes all the possible types in the schema that satisfy [typeSet].
 * @param actualPossibleTypes the actual types that will resolve to this shape. This is different from [schemaPossibleTypes]
 * as a given type will resolve to the most qualified shape
 */
private data class ShapeInternal(val typeSet: TypeSet, val schemaPossibleTypes: PossibleTypes, val actualPossibleTypes: PossibleTypes)
