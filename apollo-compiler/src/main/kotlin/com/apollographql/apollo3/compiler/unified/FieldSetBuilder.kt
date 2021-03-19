package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLArgument
import com.apollographql.apollo3.compiler.frontend.GQLField
import com.apollographql.apollo3.compiler.frontend.GQLFieldDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo3.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo3.compiler.frontend.GQLSelection
import com.apollographql.apollo3.compiler.frontend.GQLType
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.coerce
import com.apollographql.apollo3.compiler.frontend.definitionFromScope
import com.apollographql.apollo3.compiler.frontend.findDeprecationReason


/**
 * For a [TypedSelectionSet]  collect all the typeConditions.
 * Then for each combination of typeConditions, collect all the fields recursively.
 *
 * While doing so, it records all the used fragments and used types
 *
 * @param registerFragment a callback to register fragments as we encounter them.
 * @param registerType a factory for IrType. This is used to track what types are used to only generate those
 */
class IrFieldSetBuilder(
    private val schema: Schema,
    private val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val baseSelections: List<GQLSelection>,
    private val baseType: String,
    private val registerFragment: (String) -> Unit,
    private val registerType: (GQLType) -> IrType,
) {

  class TypedSelectionSet(
      val selections: List<GQLSelection>,
      val selectionSetTypeCondition: String,
  )

  fun build(): List<IrFieldSet> {
    return TypedSelectionSet(baseSelections, baseType).toIrFieldSets()
  }

  private fun TypedSelectionSet.toIrFieldSets(): List<IrFieldSet> {
    val fragmentCollectionResult = FragmentCollectionScope(selections, selectionSetTypeCondition, allGQLFragmentDefinitions).collect()

    fragmentCollectionResult.namedFragments.forEach {
      registerFragment(it.name)
    }

    val interfacesTypeSets = fragmentCollectionResult.typeSet

    val shapeTypeSetToPossibleTypes = computeShapes(schema, interfacesTypeSets.union())

    val interfaceFieldSets = interfacesTypeSets.map {
      toIrFieldSet(
          typeSet = it,
          possibleTypes = emptySet(),
          superTypeSets = emptySet(),
          namedFragments = emptySet()
      )
    }

    val shapesTypeSets = shapeTypeSetToPossibleTypes.keys

    val shapesFieldSets = shapesTypeSets.map { shapeTypeSet ->
      /**
       * TODO: we need to iterate on variables too to support @include directives
       */
      val namedFragments = fragmentCollectionResult
          .namedFragments
          .filter {
            it.condition.evaluate(emptySet(), shapeTypeSet)
          }
          .map { it.name }
          .toSet()

      toIrFieldSet(
          typeSet = shapeTypeSet,
          possibleTypes = shapeTypeSetToPossibleTypes[shapeTypeSet]!!,
          superTypeSets = interfacesTypeSets.filter { shapeTypeSet.implements(it) }.toSet(),
          namedFragments = namedFragments
      )
    }

    return interfaceFieldSets + shapesFieldSets
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

  private fun TypedSelectionSet.collectFields(typeSet: TypeSet): List<CollectedField> {
    if (!typeSet.contains(selectionSetTypeCondition)) {
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
                  type = registerType(fieldDefinition.type),
                  description = fieldDefinition.description,
                  deprecationReason = fieldDefinition.directives.findDeprecationReason(),
              )
          )
        }
        is GQLInlineFragment -> {
          TypedSelectionSet(it.selectionSet.selections, it.typeCondition.name).collectFields(typeSet)
        }
        is GQLFragmentSpread -> {
          val fragment = allGQLFragmentDefinitions[it.name]!!
          TypedSelectionSet(fragment.selectionSet.selections, fragment.typeCondition.name).collectFields(typeSet)
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
        type = registerType(argumentDefinition.type)
    )
  }

  private fun TypedSelectionSet.toIrFieldSet(
      typeSet: TypeSet,
      possibleTypes: PossibleTypes,
      superTypeSets: Set<TypeSet>,
      namedFragments: Set<String>,
  ): IrFieldSet {
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
        superTypeSets = superTypeSets,
        fields = fields,
        namedFragments = namedFragments
    )
  }


}
