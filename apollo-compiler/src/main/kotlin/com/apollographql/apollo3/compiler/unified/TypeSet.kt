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
 * Return the different possible shapes for all concrete types that satisfy [fieldType]
 *
 */
internal fun computeShapes(schema: Schema, fieldType: String, typeConditions: Set<String>): Map<TypeSet, PossibleTypes> {
  val possibleTypes = schema.typeDefinition(fieldType).possibleTypes(schema)

  val typeConditionToPossibleTypes = typeConditions.map {
    it to schema.typeDefinition(it).possibleTypes(schema).intersect(possibleTypes)
  }

  return schema.typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>()
      .map { it.name }
      .map { concreteType ->
        val matchedSupers = typeConditionToPossibleTypes.filter { it.second.contains(concreteType) }
            .map { it.first }
            .toSet()

        matchedSupers to concreteType
      }
      .filter { it.first.isNotEmpty() }
      .groupBy(
          keySelector = { it.first },
          valueTransform = { it.second }
      )
      .mapValues { it.value.toSet() }
}

internal fun TypeSet.implements(other: TypeSet) = intersect(other) == other
