package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.possibleTypes

internal data class Bucket(val typeSet: TypeSet, val possibleTypes: PossibleTypes)

/**
 * This function distributes [incomingTypes] into different buckets.
 */
internal fun buckets(
    schema: Schema,
    incomingTypes: List<String>,
    userTypeSets: List<TypeSet>,
): List<Bucket> {

  val typeConditionToPossibleTypes = userTypeSets.union().associateWith {
    schema.typeDefinition(it).possibleTypes(schema)
  }

  /**
   * Start with the user type sets
   */
  val buckets = userTypeSets.map { userTypeSet ->
    BucketInternal(
        userTypeSet,
        userTypeSet.map { typeConditionToPossibleTypes[it]!! }.intersection(),
        emptySet()
    )
  }.toMutableList()

  incomingTypes.forEach { concreteType ->
    val superShapes = mutableListOf<BucketInternal>()
    buckets.forEachIndexed { index, shape ->
      if (buckets[index].schemaPossibleTypes.contains(concreteType)) {
        superShapes.add(shape)
      }
    }

    if (superShapes.isEmpty()) {
      // This incoming type is never selected
      return@forEach
    }

    /**
     * Take all superShapes of a given type. This type will use a shape matching the union of all the superShapes (most qualified typeset).
     *
     * It might be that the user did not specifically require this particular typeSet. For an example, with following type conditions:
     * [A, B]
     * [A, C]
     *
     * A concrete object matching [A, B, C] needs a new shape.
     */
    val bucketTypeSet = superShapes.fold(emptySet<String>()) { acc, shape ->
      acc.union(shape.typeSet)
    }

    val index = buckets.indexOfFirst { it.typeSet == bucketTypeSet }
    if (index < 0) {
      buckets.add(
          BucketInternal(
              bucketTypeSet,
              bucketTypeSet.map { typeConditionToPossibleTypes[it]!! }.intersection(),
              setOf(concreteType)
          )
      )
    } else {
      val existingShape = buckets[index]
      buckets[index] = existingShape.copy(actualPossibleTypes = existingShape.actualPossibleTypes + concreteType)
    }
  }

  /**
   * Filter out empty buckets
   */
  return buckets.filter { it.actualPossibleTypes.isNotEmpty() }.map { Bucket(it.typeSet, it.actualPossibleTypes) }
}

/**
 * @param schemaPossibleTypes all the possible types in the schema that satisfy [typeSet].
 * @param actualPossibleTypes the actual types that will resolve to this shape. This is different from [schemaPossibleTypes]
 * as a given type will fall into the most qualified bucket
 */
private data class BucketInternal(val typeSet: TypeSet, val schemaPossibleTypes: PossibleTypes, val actualPossibleTypes: PossibleTypes)
