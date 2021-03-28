package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.PackageNameProvider
import com.apollographql.apollo3.compiler.backend.codegen.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForFragmentImplementation
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForOperation
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
import com.apollographql.apollo3.compiler.frontend.pretty

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
    private val packageNameProvider: PackageNameProvider,
    private val registerType: (GQLType) -> IrType,
) {
  private var cachedFragmentsFields = mutableMapOf<String, IrField>()

  fun buildOperation(
      selections: List<GQLSelection>,
      fieldType: String,
      name: String,
  ): IrField {
    val path = ModelPath(packageNameProvider.operationPackageName(
        selections.filePath()),
        listOf(kotlinNameForOperation(name))
    )

    return buildRootField(
        name = "data",
        selections = selections,
        fieldType = fieldType,
        path = path
    )
  }

  /**
   * TODO: find a more robust way to get the filePath
   *
   * Note: there might be synthetic selections with filePath == null for __typename fields
   */
  private fun List<GQLSelection>.filePath() = mapNotNull { it.sourceLocation.filePath }.first()


  fun getOrBuildFragmentField(
      selections: List<GQLSelection>,
      fieldType: String,
      name: String,
  ): IrField {
    val path = ModelPath(packageNameProvider.fragmentPackageName(
        selections.filePath()),
        emptyList()
    )

    return cachedFragmentsFields.getOrPut(name) {
      buildRootField(
          name = name,
          selections = selections,
          fieldType = fieldType,
          path = path
      )
    }
  }

  class FragmentFields(val interfaceField: IrField, val dataField: IrField)

  fun buildFragment(
      selections: List<GQLSelection>,
      fieldType: String,
      name: String,
  ): FragmentFields {
    val interfaceField = getOrBuildFragmentField(selections, fieldType, name)

    val dataPath = ModelPath(
        packageNameProvider.fragmentPackageName(selections.filePath()),
        listOf(kotlinNameForFragmentImplementation(name))
    )
    val dataField = buildRootField(
        name = "data",
        selections = selections,
        fieldType = fieldType,
        path = dataPath,
        superFields = listOf(interfaceField)
    )
    return FragmentFields(interfaceField, dataField)
  }

  private fun buildRootField(
      name: String,
      selections: List<GQLSelection>,
      fieldType: String,
      path: ModelPath,
      superFields: List<IrField> = emptyList(),
  ): IrField {
    return buildField(
        name = name,
        alias = null,
        description = null,
        deprecationReason = null,
        arguments = emptyList(),
        selections = selections,
        type = GQLNamedType(SourceLocation.UNKNOWN, fieldType),
        superFields = superFields,
        path = path,
        condition = BooleanExpression.True,
    )
  }

  private fun modelName(typeSet: TypeSet, fieldType: String, responseName: String): String {
    return ((typeSet - fieldType).sorted() + responseName).map { capitalizeFirstLetter(it) }.joinToString("")
  }

  private fun List<GQLSelection>.collectConditions(typeSet: TypeSet): List<String> {
    return typeSet.toList() + flatMap {
      when (it) {
        is GQLField -> emptyList()
        is GQLInlineFragment -> it.selectionSet.selections.collectConditions(typeSet + it.typeCondition.name)
        is GQLFragmentSpread -> {
          val fragmentDefinition = allGQLFragmentDefinitions[it.name]!!
          fragmentDefinition.selectionSet.selections.collectConditions(typeSet + fragmentDefinition.typeCondition.name)
        }
      }
    }
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
    val fieldSets: List<IrFieldSet>
    val interfaceFieldSets: List<IrFieldSet>
    val implementationFieldSets: List<IrFieldSet>
    val inlineAccessors: List<IrInlineAccessor>
    val fragmentAccessors: List<IrFragmentAccessor>

    val fieldType = type.leafType().name

    if (selections.isNotEmpty()) {
      val typeConditions = selections.collectConditions(setOf(fieldType)).toSet()

      val shapeTypeSetToPossibleTypes = computeShapes(schema, fieldType, typeConditions)

      /**
       * Always add the base fieldType in case new types are added to the schema
       */
      val shapesTypeSets = shapeTypeSetToPossibleTypes.keys + setOf(setOf(fieldType))

      /**
       * Generate the common interfaces
       * We need those to access the shapes in a generic way
       */
      val commonTypeSets = shapesTypeSets.toList().pairs()
          .map { it.first.intersect(it.second) }
          .filter { it.isNotEmpty() }
          .toSet()

      val fragmentFields = selections.filterIsInstance<GQLFragmentSpread>().map { fragmentSpread ->
        val gqlFragmentDefinition = allGQLFragmentDefinitions[fragmentSpread.name]!!
        getOrBuildFragmentField(
            selections = gqlFragmentDefinition.selectionSet.selections,
            fieldType = gqlFragmentDefinition.typeCondition.name,
            name = fragmentSpread.name,
        )
      }

      /**
       * Create a cache of interface IrFieldSets so that we don't end up building the same FieldSets all the time
       */
      val cachedFieldSets = mutableMapOf<TypeSet, IrFieldSet>()

      val responseName = alias ?: name

      val allTypeSets = commonTypeSets + shapesTypeSets

      /**
       * Build the field sets starting from the less qualified so we can look up the super
       * interfaces in cachedFieldSets when needed
       */
      fieldSets = allTypeSets.sortedBy { it.size }.map { typeSet ->
        val modelName = modelName(typeSet, fieldType, responseName)

        val superFieldSet = cachedFieldSets.values
            .sortedByDescending { it.typeSet.size }
            .firstOrNull {
              it.typeSet.size < typeSet.size && typeSet.implements(it.typeSet)
            }

        val superFragmentFieldSets = fragmentFields.mapNotNull { field ->
          field.fieldSets.sortedByDescending { it.typeSet.size }
              .firstOrNull {
                typeSet.implements(it.typeSet)
              }
        }

        val relatedFieldSets = superFields.mapNotNull { field ->
          field.fieldSets.sortedByDescending { it.typeSet.size }
              .firstOrNull {
                typeSet.implements(it.typeSet)
              }
        }

        buildFieldSet(
            selections = selections,
            fieldType = fieldType,
            typeSet = typeSet,
            modelName = modelName,
            possibleTypes = shapeTypeSetToPossibleTypes[typeSet] ?: emptySet(),
            superFieldSets = superFragmentFieldSets + listOfNotNull(superFieldSet) + relatedFieldSets,
            path = path,
        ).also {
          cachedFieldSets[typeSet] = it
        }
      }

      val otherFieldSets = commonTypeSets.intersect(shapesTypeSets).map { typeSet ->
        val modelName = "Other${modelName(typeSet, fieldType, responseName)}"
        buildFieldSet(
            selections = selections,
            fieldType = fieldType,
            typeSet = typeSet,
            modelName = modelName,
            possibleTypes = shapeTypeSetToPossibleTypes[typeSet] ?: emptySet(),
            superFieldSets = listOf(cachedFieldSets[typeSet]!!),
            path = path,
        )
      }

      interfaceFieldSets = fieldSets.filter {
        commonTypeSets.contains(it.typeSet)
      }.sortedBy { it.typeSet.size }

      implementationFieldSets = otherFieldSets + fieldSets.filter {
        (shapesTypeSets - commonTypeSets).contains(it.typeSet)
      }.sortedBy { it.typeSet.size }

      inlineAccessors = (allTypeSets - setOf(setOf(fieldType))).map { accessorTypeSet ->
        IrInlineAccessor(
            typeSet = accessorTypeSet,
            override = (superFields + fragmentFields).any {
              it.inlineAccessors.any {
                it.typeSet == accessorTypeSet
              }
            },
            path = (implementationFieldSets.firstOrNull { it.typeSet == accessorTypeSet }
                ?: fieldSets.first { it.typeSet == accessorTypeSet }).fullPath
        )
      }
      fragmentAccessors = selections.filterIsInstance<GQLFragmentSpread>().distinctBy { it.name }.map { fragmentSpread ->
        IrFragmentAccessor(
            name = fragmentSpread.name,
            override = (superFields + fragmentFields).any { it.fragmentAccessors.any { it.name == fragmentSpread.name } },
            path = cachedFragmentsFields[fragmentSpread.name]?.typeFieldSet?.fullPath
                ?: error("cannot find fragment ${fragmentSpread.name}")
        )
      }
    } else {
      fieldSets = emptyList()
      interfaceFieldSets = emptyList()
      implementationFieldSets = emptyList()
      inlineAccessors = emptyList()
      fragmentAccessors = emptyList()
    }

    val typeFieldSet = interfaceFieldSets.firstOrNull() ?: implementationFieldSets.firstOrNull()

    return IrField(
        alias = alias,
        name = name,
        arguments = arguments,
        description = description,
        deprecationReason = deprecationReason,
        type = registerType(type),
        condition = condition,
        override = superFields.isNotEmpty(),

        typeFieldSet = typeFieldSet,
        fieldSets = fieldSets,
        interfaces = interfaceFieldSets,
        implementations = implementationFieldSets,
        inlineAccessors = inlineAccessors,
        fragmentAccessors = fragmentAccessors
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
      typeSet: TypeSet,
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
        type = registerType(argumentDefinition.type)
    )
  }

  private fun buildFieldSet(
      selections: List<GQLSelection>,
      fieldType: String,
      typeSet: TypeSet,
      modelName: String,
      possibleTypes: PossibleTypes,
      superFieldSets: List<IrFieldSet>,
      path: ModelPath,
  ): IrFieldSet {
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
      // TODO: The same field can have different descriptions in two different objects in which case
      // we should certainly use the interface description
      // check(fieldsWithSameResponseName.map { it.description }.distinct().size == 1)
      check(fieldsWithSameResponseName.map { it.deprecationReason }.distinct().size == 1)
      // GQLTypes might differ because of their source location. Use pretty()
      // to canonicalize them
      check(fieldsWithSameResponseName.map { it.type }.distinctBy { it.pretty() }.size == 1)

      val first = fieldsWithSameResponseName.first()
      val childSelections = fieldsWithSameResponseName.flatMap { it.selections }

      val superFields = superFieldSets.mapNotNull {
        it.fields.firstOrNull { it.responseName == first.responseName }
      }

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
          path = path + modelName,
      )
    }

    val implements = superFieldSets.map { it.fullPath }.toSet()

    return IrFieldSet(
        modelName = modelName,
        typeSet = typeSet,
        possibleTypes = possibleTypes,
        fields = fields,
        implements = implements,
        path = path,
    )
  }
}
