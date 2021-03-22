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
import com.apollographql.apollo3.compiler.frontend.leafType
import com.apollographql.apollo3.compiler.unified.IrFieldSetBuilder.TypedSelectionSet


/**
 * For a [TypedSelectionSet]  collect all the typeConditions.
 * Then for each combination of typeConditions, collect all the fields recursively.
 *
 * While doing so, it records all the used fragments and used types
 *
 * @param registerType a factory for IrType. This is used to track what types are used to only generate those
 */
class IrFieldSetBuilder(
    private val schema: Schema,
    private val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val registerType: (GQLType, IrFieldSet?) -> IrType,
) {

  private var cachedFragments = mutableMapOf<String, IrField>()

  class TypedSelectionSet(
      val selections: List<GQLSelection>,
      val selectionSetTypeCondition: String,
  )

  fun buildOperation(
      typedSelectionSet: TypedSelectionSet,
      name: String,
      packageName: String,
  ): IrField {
    return buildDataField(
        typedSelectionSet = typedSelectionSet,
        path = ModelPath(packageName, ModelPath.Root.Operation(name))
    )
  }

  fun buildFragment(
      name: String,
      typedSelectionSet: TypedSelectionSet,
      packageName: String,
  ): IrField {
    return cachedFragments.getOrPut(name) {
      buildDataField(
          typedSelectionSet = typedSelectionSet,
          path = ModelPath(packageName, ModelPath.Root.Fragment(name))
      )
    }
  }

  private fun buildDataField(
      typedSelectionSet: TypedSelectionSet,
      path: ModelPath
  ): IrField {

    val fieldSets = buildIrFieldSets(
        typedSelectionSet = typedSelectionSet,
        superFieldSets = emptyList(),
        path = path,
        responseName = "data"
    )

    return IrField(
        name = "data",
        alias = null,
        deprecationReason = null,
        arguments = emptyList(),
        type = IrCompoundType(fieldSets.firstOrNull { it.typeSet.size == 1 }!!),
        condition = BooleanExpression.True,
        description = "Synthetic data field",
        fieldSets = fieldSets,
        override = false
    )
  }

  private fun buildIrFieldSets(
      typedSelectionSet: TypedSelectionSet,
      superFieldSets: List<IrFieldSet>,
      path: ModelPath,
      responseName: String
  ): List<IrFieldSet> {
    val collectionResult = FragmentCollectionScope(typedSelectionSet.selections, typedSelectionSet.selectionSetTypeCondition, allGQLFragmentDefinitions).collect()

    val typeConditions = collectionResult.typeSet.union()

    val shapeTypeSetToPossibleTypes = computeShapes(schema, typeConditions)
    val shapesTypeSets = shapeTypeSetToPossibleTypes.keys

    /**
     * Generate the common interfaces
     * We need those to access the shapes in a generic way
     */
    val interfaceTypeSetsToGenerate = shapesTypeSets.toList().combinations()
        .filter {
          it.size >= 2
        }
        .map {
          it.intersection()
        }
        .toSet()

    val allTypeSets = (interfaceTypeSetsToGenerate + shapesTypeSets)

    val fieldSetCache = mutableMapOf<TypeSet, IrFieldSet>()

    allTypeSets.forEach { typeSet ->
      buildFieldSet(
          fieldSetCache = fieldSetCache,
          allTypeSets = allTypeSets,
          typedSelectionSet = typedSelectionSet,
          typeSet = typeSet,
          responseName = responseName,
          shapeTypeSetToPossibleTypes = shapeTypeSetToPossibleTypes,
          superFieldSets = superFieldSets,
          path = path,
      )
    }

    return fieldSetCache.values.toList()
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
      val type: GQLType,
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
                  type = fieldDefinition.type,
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
        type = registerType(argumentDefinition.type, null)
    )
  }

  private fun buildFieldSet(
      fieldSetCache: MutableMap<TypeSet, IrFieldSet>,
      allTypeSets: Set<Set<String>>,
      typedSelectionSet: TypedSelectionSet,
      typeSet: TypeSet,
      responseName: String,
      shapeTypeSetToPossibleTypes: Map<TypeSet, PossibleTypes>,
      superFieldSets: List<IrFieldSet>,
      path: ModelPath
  ): IrFieldSet {
    if (fieldSetCache[typeSet] != null) {
      return fieldSetCache[typeSet]!!
    }

    val superTypeSet = allTypeSets.filter {
      it.size < typeSet.size
    }.sortedByDescending { it.size }
        .firstOrNull { typeSet.implements(it) }

    val superFieldSet = superTypeSet?.let {
      buildFieldSet(
          fieldSetCache,
          allTypeSets,
          typedSelectionSet,
          it,
          responseName,
          shapeTypeSetToPossibleTypes,
          superFieldSets,
          path,
      )
    }

    val collectedFields = typedSelectionSet.collectFields(typeSet)

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

      val cousinFields = (superFieldSets + superFieldSet)
          .filterNotNull()
          .mapNotNull { it.fields.firstOrNull { it.responseName == first.responseName } }

      val fieldSets = buildIrFieldSets(
          typedSelectionSet = TypedSelectionSet(selections, first.type.leafType().name),
          superFieldSets = cousinFields.mapNotNull { it.baseFieldSet },
          path = path + PathElement(typeSet, typedSelectionSet.selectionSetTypeCondition, responseName),
          responseName = first.responseName
      )

      val baseFieldSet = fieldSets.firstOrNull { it.typeSet.size == 1 }
      
      IrField(
          alias = first.alias,
          name = first.name,
          arguments = first.arguments,
          description = first.description,
          deprecationReason = first.deprecationReason,
          type = registerType(first.type, baseFieldSet),
          condition = BooleanExpression.Or(fieldsWithSameResponseName.map { it.condition }.toSet()),
          fieldSets = fieldSets,
          override = cousinFields.isNotEmpty()
      )
    }

    val implements = if (superFieldSet == null) {
      // this is a baseFieldSet, pull the superFieldSets
      superFieldSets
    } else {
      // this was already pulled by the baseFieldSet, no need to add superFieldSets
      listOf(superFieldSet)
    }.map { it.fullPath }.toSet()

    val fieldSet = IrFieldSet(
        typeSet = typeSet.toSet(),
        fieldType = typedSelectionSet.selectionSetTypeCondition,
        possibleTypes = shapeTypeSetToPossibleTypes[typeSet] ?: emptySet(),
        fields = fields,
        implements = implements,
        path = path,
        responseName = responseName
    )

    return fieldSet.also {
      fieldSetCache[typeSet] = it
    }
  }
}
