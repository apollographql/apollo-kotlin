package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLArgument
import com.apollographql.apollo3.compiler.frontend.GQLField
import com.apollographql.apollo3.compiler.frontend.GQLFieldDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo3.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo3.compiler.frontend.GQLNamedType
import com.apollographql.apollo3.compiler.frontend.GQLSelection
import com.apollographql.apollo3.compiler.frontend.GQLType
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.SourceLocation
import com.apollographql.apollo3.compiler.frontend.coerce
import com.apollographql.apollo3.compiler.frontend.definitionFromScope
import com.apollographql.apollo3.compiler.frontend.findDeprecationReason
import com.apollographql.apollo3.compiler.frontend.leafType

/**
 * For a list of selections collect all the typeConditions.
 * Then for each combination of typeConditions, collect all the fields recursively.
 *
 * While doing so, record all the used fragments and used types
 *
 * @param registerType a factory for IrType. This is used to track what types are used to only generate those
 */
class IrFieldSetBuilder(
    private val schema: Schema,
    private val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val registerType: (GQLType, IrFieldSet?) -> IrType,
) {
  private var cachedFragments = mutableMapOf<String, IrField>()

  fun buildOperation(
      selections: List<GQLSelection>,
      fieldType: String,
      name: String,
      packageName: String,
  ): IrField {
    return buildDataField(
        selections = selections,
        fieldType = fieldType,
        path = ModelPath(packageName, ModelPath.Root.Operation(name))
    )
  }

  fun buildFragment(
      selections: List<GQLSelection>,
      fieldType: String,
      name: String,
      packageName: String,
  ): IrField {
    return cachedFragments.getOrPut(name) {
      buildDataField(
          selections = selections,
          fieldType = fieldType,
          path = ModelPath(packageName, ModelPath.Root.Fragment(name))
      )
    }
  }

  private fun buildDataField(
      selections: List<GQLSelection>,
      fieldType: String,
      path: ModelPath
  ): IrField {
    return buildField(
        name = "data",
        alias = null,
        description = null,
        deprecationReason = null,
        arguments = emptyList(),
        selections = selections,
        type = GQLNamedType(SourceLocation.UNKNOWN, fieldType),
        superFields = emptyList(),
        path = path,
        condition = BooleanExpression.True,
    )
  }

  private fun buildField(
      alias: String?,
      name: String,
      arguments: List<IrArgument>,
      description: String?,
      deprecationReason: String?,
      type: GQLType,
      condition: BooleanExpression,
      selections: List<GQLSelection>,
      superFields: List<IrField>,
      path: ModelPath,
  ): IrField {
    val collectionResult = FragmentCollectionScope(selections, type.leafType().name, allGQLFragmentDefinitions).collect()

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

    val responseName = alias ?: name
    /**
     * Build the interfaces starting from the less qualified so we can look up the super
     * interfaces in fieldSetCache when needed
     */
    val interfaceTypeSets = interfaceTypeSetsToGenerate.sortedBy { it.size }.map { typeSet ->
      buildFieldSet(
          fieldSetCache = fieldSetCache,
          allTypeSets = allTypeSets,
          selections = selections,
          fieldType = type.leafType().name,
          typeSet = typeSet,
          responseName = responseName,
          shapeTypeSetToPossibleTypes = shapeTypeSetToPossibleTypes,
          superFieldSets = superFields.mapNotNull { it.baseFieldSet },
          path = path,
      )
    }

    val allFieldSets = fieldSetCache.values.toList()
    val implementationsFieldSets = allFieldSets.filter {
      it.possibleTypes.isNotEmpty()
    }
    val baseFieldSet = fieldSetCache[setOf(type.leafType().name)]

    return IrField(
          alias = alias,
          name = name,
          arguments = arguments,
          description = description,
          deprecationReason = deprecationReason,
          type = registerType(type, baseFieldSet),
          condition = condition,
          fieldSets = allFieldSets,
          override = superFields.isNotEmpty()
      )
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

  private fun collectFields(
      selections: List<GQLSelection>,
      typeCondition: String,
      typeSet: TypeSet
  ): List<CollectedField> {
    if (!typeSet.contains(typeCondition)) {
      return emptyList()
    }
    val typeDefinition = schema.typeDefinition(typeCondition)

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
          collectFields(it.selectionSet.selections, it.typeCondition.name, typeSet)
        }
        is GQLFragmentSpread -> {
          val fragment = allGQLFragmentDefinitions[it.name]!!
          collectFields(fragment.selectionSet.selections, fragment.typeCondition.name, typeSet)
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
      selections: List<GQLSelection>,
      fieldType: String,
      typeSet: TypeSet,
      responseName: String,
      shapeTypeSetToPossibleTypes: Map<TypeSet, PossibleTypes>,
      superFieldSets: List<IrFieldSet>,
      path: ModelPath
  ): IrFieldSet {
    val superTypeSet = allTypeSets.filter {
      it.size < typeSet.size
    }.sortedByDescending { it.size }
        .firstOrNull { typeSet.implements(it) }

    val superFieldSet = if (superTypeSet != null)  {
      buildFieldSet(
          fieldSetCache,
          allTypeSets,
          selections,
          fieldType,
          superTypeSet,
          responseName,
          shapeTypeSetToPossibleTypes,
          superFieldSets,
          path,
      )
    } else {
      null
    }

    val collectedFields = collectFields(selections, fieldType, typeSet)

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
      val childSelections = fieldsWithSameResponseName.flatMap { it.selections }

      val superFields = (superFieldSets + superFieldSet)
          .filterNotNull()
          .mapNotNull { it.fields.firstOrNull { it.responseName == first.responseName } }

      buildField(
          alias = first.alias,
          name = first.name,
          arguments = first.arguments,
          description = first.description,
          deprecationReason = first.deprecationReason,
          type = first.type,
          condition = BooleanExpression.Or(fieldsWithSameResponseName.map { it.condition }.toSet()),
          selections = childSelections,
          superFields = superFields,
          path = path + PathElement(typeSet, fieldType, responseName),
      )
    }

    val finalSuperFieldSets = if (superFieldSet == null) {
      // this is a baseFieldSet, pull the superFieldSets
      superFieldSets
    } else {
      // this was already pulled by the baseFieldSet, no need to add superFieldSets
      listOf(superFieldSet)
    }

    finalSuperFieldSets.forEach {
      it.usageCount++
    }

    val implements = finalSuperFieldSets.map { it.fullPath }.toSet()

    return IrFieldSet(
        typeSet = typeSet.toSet(),
        responseName = responseName,
        fieldType = fieldType,
        possibleTypes = shapeTypeSetToPossibleTypes[typeSet] ?: emptySet(),
        fields = fields,
        implements = implements,
        path = path,
    ).also {
      fieldSetCache[typeSet] = it
    }
  }
}
