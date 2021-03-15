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

  /**
   * Return a list of set of type conditions.
   *
   * Each individual set matches a path for which we need to potentially generate a model
   *
   * {# Base type A
   *   ... on B {
   *     ... on C {
   *   }
   *   ... on B {
   *     ... on D {
   *   }
   * }
   *
   * Will return:
   * [
   *   [A]
   *   [A,B]
   *   [A,B,C]
   *   [A,B,D]
   * ]
   */
  private fun List<GQLSelection>.collectTypeConditions(path: Set<String>): List<Set<String>> {
    return listOf(path) + flatMap {
      when (it) {
        is GQLField -> emptyList()
        is GQLInlineFragment -> it.selectionSet.selections.collectTypeConditions(path + it.typeCondition.name)
        is GQLFragmentSpread -> {
          val fragmentDefinition = allGQLFragmentDefinitions[it.name]!!
          usedNamedFragments.add(it.name)
          fragmentDefinition.selectionSet.selections.collectTypeConditions(path + fragmentDefinition.typeCondition.name)
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
    val interfacesTypeSets = selections.collectTypeConditions(setOf(selectionSetTypeCondition)).toSet()

    val shapeTypeSetToPossibleTypes = computeShapes(schema, interfacesTypeSets.union())

    val shapesTypeSets = shapeTypeSetToPossibleTypes.keys

    /**
     * build the relations of shape to their interfaceTypeSets
     *
     * By construction, shapes do not inherit each other
     */
    var shapeToFragmentEdges = interfacesTypeSets.flatMap { interfaceTypeSet ->
      shapesTypeSets.filter { shapeTypeSet ->
        shapeTypeSet.implements(interfaceTypeSet)
      }.map { shapesTypeSet ->
        Edge(
            source = ShapeNode(shapesTypeSet),
            target = InterfaceNode(interfaceTypeSet)
        )
      }
    }

    /**
     * Prune interfaceTypeSets that are only implemented by one shape or less, we won't need those
     */
    val interfaceTypeSetsToGenerate = shapeToFragmentEdges.groupBy(
        keySelector = { it.target },
        valueTransform = { it.source }
    ).filter {
      it.value.size > 1
    }.keys

    shapeToFragmentEdges = shapeToFragmentEdges.filter {
      interfaceTypeSetsToGenerate.contains(it.target)
    }

    /**
     * build the internal relations of the different interfaceTypeSets
     */
    val fragmentToFragmentEdges = interfacesTypeSets.pairs().mapNotNull {
      when {
        it.first.implements(it.second) -> Edge(InterfaceNode(it.first), InterfaceNode(it.second))
        it.second.implements(it.first) -> Edge(InterfaceNode(it.second), InterfaceNode(it.first))
        else -> {
          null
        }
      }
    }

    /**
     * Try to simplify inheritance relations
     */
    val edges = transitiveReduce(shapeToFragmentEdges + fragmentToFragmentEdges)

    edges.flatMap { listOf(it.source, it.target) }.distinct().forEach {
      toIrFieldSet(
          typeSet = it.typeSet,
          possibleTypes = (it as? ShapeNode)?.typeSet?.let { shapeTypeSetToPossibleTypes[it] } ?: emptySet(),
          implements = edges.filter { edge -> edge.source == it }.map { it.target.typeSet }.toSet()
      )
    }
    return emptyList()
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

  private fun TypedSelectionSet.collectFields(typeConditions: TypeSet): List<CollectedField> {
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

  private fun TypedSelectionSet.toIrFieldSet(typeSet: TypeSet, possibleTypes: PossibleTypes, implements: Set<TypeSet>): IrFieldSet {
    val collectedFields = collectFields(typeSet)

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
        typeSet = typeSet.toSet(),
        possibleTypes = possibleTypes,
        implements = implements,
        fields = fields
    )
  }


}
