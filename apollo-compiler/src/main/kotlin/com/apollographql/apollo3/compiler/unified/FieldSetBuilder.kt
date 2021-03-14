package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLArgument
import com.apollographql.apollo3.compiler.frontend.GQLField
import com.apollographql.apollo3.compiler.frontend.GQLFieldDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo3.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo3.compiler.frontend.GQLSelection
import com.apollographql.apollo3.compiler.frontend.GQLSelectionSet
import com.apollographql.apollo3.compiler.frontend.GQLType
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.coerce
import com.apollographql.apollo3.compiler.frontend.definitionFromScope
import com.apollographql.apollo3.compiler.frontend.findDeprecationReason
import com.apollographql.apollo3.compiler.frontend.possibleTypes
import java.math.BigInteger

/**
 * For a "base" selectionSet (either query, named fragment or field), collect all the typeConditions
 * Then for each combination of typeConditions, collect all the fields recursively
 */
class FieldSetsBuilder(
    val schema: Schema,
    val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    val baseSelectionSet: GQLSelectionSet,
    val baseTypeCondition: String,
    val toIrType: GQLType.() -> IrType
) {
  class Result(
      val fieldSets: List<IrFieldSet>,
      val usedNamedFragments: Set<String>,
  )

  private var usedNamedFragments = mutableListOf<String>()

  private fun List<GQLSelection>.collectTypeConditions(): List<String> {
    return flatMap {
      when (it) {
        is GQLField -> emptyList()
        is GQLInlineFragment -> listOf(it.typeCondition.name) + it.selectionSet.selections.collectTypeConditions()
        is GQLFragmentSpread -> {
          val fragmentDefinition = allGQLFragmentDefinitions[it.name]!!
          usedNamedFragments.add(it.name)
          listOf(fragmentDefinition.typeCondition.name) + fragmentDefinition.selectionSet.selections.collectTypeConditions()
        }
      }
    }
  }

  fun build(): Result {
    return Result(
        fieldSets = TypedSelectionSet(baseSelectionSet.selections, baseTypeCondition).toIrFieldSets(),
        usedNamedFragments = usedNamedFragments.toSet(),
    )
  }

  private fun TypedSelectionSet.toIrFieldSets(): List<IrFieldSet> {
    val typeConditions = selections.collectTypeConditions()

    return typeConditions.combinations().map {
      toIrFieldSet(it)
    }
  }
  private fun possibleTypes(typeConditions: List<String>): Set<String> {
    return typeConditions.map {
      schema.typeDefinition(it).possibleTypes(schema.typeDefinitions)
    }.intersection()
  }

  /**
   * An intermediate class used during collection
   */
  private class CollectedField(
      /**
       * All fields with the same response name should have the same infos here
       */
      val name: String,
      val alias: String?,
      val arguments: List<IrArgument>,

      val description: String?,
      val type: IrType,
      val deprecationReason: String?,

      /**
       * Merged field will merge their conditions and selectionSets
       */
      val condition: BooleanExpression,
      val selections: List<GQLSelection>,
  ) {
    val responseName = alias ?: name
  }

  private class TypedSelectionSet(val selections: List<GQLSelection>, val selectionSetTypeCondition: String)

  private fun TypedSelectionSet.collectFields(typeConditions: List<String>): List<CollectedField> {
    if (!typeConditions.contains(selectionSetTypeCondition)) {
      return emptyList()
    }
    val typeDefinition = schema.typeDefinition(selectionSetTypeCondition)

    return selections.flatMap {
      when (it) {
        is GQLField -> {
          val fieldDefinition = it.definitionFromScope(schema, typeDefinition)!!
          listOf(
              CollectedField(
                  name = it.name,
                  alias = it.alias,
                  arguments = it.arguments?.arguments?.map { it.toIr(fieldDefinition) } ?: emptyList(),
                  condition = it.directives.toBooleanExpression(),
                  selections = it.selectionSet?.selections ?: emptyList(),
                  type = fieldDefinition.type.toIrType(),
                  description = fieldDefinition.description,
                  deprecationReason = fieldDefinition.directives.findDeprecationReason(),
              )
          )
        }
        is GQLInlineFragment -> {
          TypedSelectionSet(it.selectionSet.selections, it.typeCondition.name).collectFields(typeConditions)
        }
        is GQLFragmentSpread -> {
          val fragment = allGQLFragmentDefinitions[it.name]!!
          TypedSelectionSet(fragment.selectionSet.selections, fragment.typeCondition.name).collectFields(typeConditions)
        }
      }
    }
  }

  private fun GQLArgument.toIr(fieldDefinition: GQLFieldDefinition): IrArgument {
    val argumentDefinition = fieldDefinition.arguments.first { it.name == name }

    return IrArgument(
        name = name,
        value = value.coerce(argumentDefinition.type, schema).orThrow().toIr(),
        defaultValue = argumentDefinition.defaultValue?.coerce(argumentDefinition.type, schema)?.orThrow()?.toIr(),
        type = argumentDefinition.type.toIrType()
    )
  }

  private fun TypedSelectionSet.toIrFieldSet(typeConditions: List<String>): IrFieldSet {
    val collectedFields = collectFields(typeConditions)

    val fields = collectedFields.groupBy {
      it.responseName
    }.values.map { fieldsWithSameResponseName ->
      /**
       * Sanity checks, might be removed as this should be done during validation
       */
      check(fieldsWithSameResponseName.map { it.alias }.distinct().size == 1)
      check(fieldsWithSameResponseName.map { it.name }.distinct().size == 1)
      check(fieldsWithSameResponseName.map { it.arguments }.distinct().size == 1)
      check(fieldsWithSameResponseName.map { it.description }.distinct().size == 1)
      check(fieldsWithSameResponseName.map { it.deprecationReason }.distinct().size == 1)
      check(fieldsWithSameResponseName.map { it.type }.distinct().size == 1)

      val first = fieldsWithSameResponseName.first()
      val selections = fieldsWithSameResponseName.flatMap { it.selections }

      val fieldSets = TypedSelectionSet(selections, first.type.leafName).toIrFieldSets()
      IrField(
          alias = first.alias,
          name = first.name,
          arguments = first.arguments,
          description = first.description,
          deprecationReason = first.deprecationReason,
          type = first.type,

          condition = BooleanExpression.Or(fieldsWithSameResponseName.map { it.condition }.toSet()),
          fieldSets = fieldSets
      )
    }
    return IrFieldSet(
        typeConditions = typeConditions.toSet(),
        possibleTypes = possibleTypes(typeConditions),
        fields = fields
    )
  }

  /**
   * Helper function to compute the intersection of multiple Sets
   */
  private fun <T> Collection<Set<T>>.intersection(): Set<T> {
    if (isEmpty()) {
      return emptySet()
    }

    return drop(1).fold(first()) { acc, list ->
      acc.intersect(list)
    }
  }

  /**
   * For a list of items, returns all possible combinations as in https://en.wikipedia.org/wiki/Combination
   */
  private fun <T> List<T>.combinations(includeEmpty: Boolean = false): List<List<T>> {
    val start = if (includeEmpty) 0 else 1
    val end = BigInteger.valueOf(2).pow(size).intValueExact()

    return start.until(end).fold(emptyList()) { acc, bitmask ->
      acc + listOf(
          0.until(size).mapNotNull { position ->
            if (bitmask.and(1.shl(position)) != 0) {
              get(position)
            } else {
              null
            }
          }
      )
    }
  }
}
