package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLObjectTypeDefinition
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.possibleTypes

/**
 * A list of type conditions resulting from evaluating multiple potentially nested fragments.
 */
internal typealias TypeSet = Set<String>
/**
 * A list of concrete types, usually used with a a [TypeSet]
 */
internal typealias PossibleTypes = Set<String>

/**
 * Return the different possible shapes
 *
 * Note that this is not linked to the fragments typeSets
 *
 * If fragment typeSets are [[A],[A,B],[A,B,C],[A,B,D]]
 *
 * A type implementing [A,B,C,D] will match to [A,B,C,D]
 */
internal fun computeShapes(schema: Schema, typeConditions: Set<String>): Map<TypeSet, PossibleTypes> {
  val typeConditionToPossibleTypes = typeConditions.map {
    it to schema.typeDefinition(it).possibleTypes(schema)
  }

  return schema.typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>()
      .map { it.name }
      .map { concreteType ->
        val matchedSupers = typeConditionToPossibleTypes.filter { it.second.contains(concreteType) }
            .map { it.first }
            .toSet()

        matchedSupers to concreteType
      }
      .groupBy(
          keySelector = { it.first },
          valueTransform = { it.second }
      )
      .mapValues { it.value.toSet() }
}

internal fun TypeSet.implements(other: TypeSet) = intersect(other) == other
