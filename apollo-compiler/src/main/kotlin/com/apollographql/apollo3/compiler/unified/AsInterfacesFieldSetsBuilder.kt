package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.backend.codegen.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.frontend.GQLField
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo3.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo3.compiler.frontend.GQLSelection
import com.apollographql.apollo3.compiler.frontend.Schema

/**
 * For a list of selections collect all the typeConditions.
 * Then for each combination of typeConditions, collect all the fields recursively.
 *
 * While doing so, record all the used fragments and used types
 *
 * @param fieldMerger a [FieldMerger] that handles the heavy lifting of collecting fields, remember their types, etc...
 */
class AsInterfacesFieldSetBuilder(
    private val schema: Schema,
    private val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldMerger: FieldMerger,
): FieldSetsBuilder {
  private var cachedFragmentsFields = mutableMapOf<String, IrField>()

  override fun buildOperationField(
      name: String,
      selections: List<GQLSelection>,
      type: IrCompoundType,
  ): IrField {
    val path = ModelPath(root = ModelPath.Root.Operation(name))

    return buildRootField(
        name = "data",
        selections = selections,
        type = type,
        path = path
    )
  }

  private fun getOrBuildFragmentField(
      selections: List<GQLSelection>,
      type: IrCompoundType,
      name: String,
  ): IrField {
    val path = ModelPath(root = ModelPath.Root.FragmentInterface(name))

    return cachedFragmentsFields.getOrPut(name) {
      buildRootField(
          name = name,
          selections = selections,
          type = type,
          path = path
      )
    }
  }

  class FragmentFields(val interfaceField: IrField, val dataField: IrField)

  fun buildFragmentFields(
      selections: List<GQLSelection>,
      type: IrCompoundType,
      name: String,
  ): FragmentFields {
    val interfaceField = getOrBuildFragmentField(selections, type, name)

    val path = ModelPath(root = ModelPath.Root.FragmentImplementation(name))
    val dataField = buildRootField(
        name = "data",
        selections = selections,
        type = type,
        path = path,
        superFields = listOf(interfaceField)
    )
    return FragmentFields(interfaceField, dataField)
  }

  private fun buildRootField(
      name: String,
      selections: List<GQLSelection>,
      type: IrCompoundType,
      path: ModelPath,
      superFields: List<IrField> = emptyList(),
  ): IrField {
    val info = IrFieldInfo(
        name = name,
        alias = null,
        description = null,
        deprecationReason = null,
        arguments = emptyList(),
        type = type,
    )

    return buildField(
        info = info,
        selections = selections,
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

  private fun List<GQLSelection>.collectFragments(typeSet: TypeSet): Set<String> {
    return flatMap {
      when (it) {
        is GQLField -> emptySet()
        is GQLInlineFragment -> {
          if (typeSet.contains(it.typeCondition.name))
            it.selectionSet.selections.collectFragments(typeSet)
          else
            emptySet()
        }
        // We do not recurse here as inheriting the first namedFragment will
        // inherit nested ones as well
        is GQLFragmentSpread -> return setOf(it.name)
      }
    }.toSet()
  }

  private fun buildField(
      info: IrFieldInfo,
      condition: BooleanExpression,
      selections: List<GQLSelection>,
      superFields: List<IrField>,
      path: ModelPath,
  ): IrField {
    val fieldSets: List<IrFieldSet>
    val interfaceFieldSets: List<IrFieldSet>
    val implementationFieldSets: List<IrFieldSet>
    val fragmentAccessors = mutableListOf<IrFragmentAccessor>()


    if (selections.isNotEmpty()) {
      val fieldType = (info.type.leafType() as? IrCompoundType)?.name ?: "Got selections on a non-compound type for ${info.responseName}"

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

      /**
       * Create a cache of interface IrFieldSets so that we don't end up building the same FieldSets all the time
       */
      val cachedFieldSets = mutableMapOf<TypeSet, IrFieldSet>()

      val responseName = info.responseName

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

        val fragmentFields = selections.collectFragments(typeSet).map { fragmentName ->
          val gqlFragmentDefinition = allGQLFragmentDefinitions[fragmentName]!!
          getOrBuildFragmentField(
              selections = gqlFragmentDefinition.selectionSet.selections,
              type = IrCompoundType(gqlFragmentDefinition.typeCondition.name),
              name = fragmentName,
          ).also {
            fragmentAccessors.add(
                IrFragmentAccessor(
                    name = fragmentName,
                    path = it.typeFieldSet!!.fullPath
                )
            )
          }
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
    } else {
      fieldSets = emptyList()
      interfaceFieldSets = emptyList()
      implementationFieldSets = emptyList()
    }

    val typeFieldSet = interfaceFieldSets.firstOrNull() ?: implementationFieldSets.firstOrNull()

    return IrField(
        info = info,
        condition = condition,
        override = superFields.isNotEmpty(),

        typeFieldSet = typeFieldSet,
        fieldSets = fieldSets,
        interfaces = interfaceFieldSets,
        implementations = implementationFieldSets,
        fragmentAccessors = fragmentAccessors.distinctBy { it.name }
    )
  }


  /**
   *
   * @param typeSet if non-null, will recurse in all inline and named fragments contained in the [TypeSet]
   */
  private fun collectFieldsInternal(
      selections: List<GQLSelection>,
      typeCondition: String,
      typeSet: TypeSet,
  ): List<FieldWithParent> {
    return selections.flatMap {
      when (it) {
        is GQLField -> listOf(FieldWithParent(it, typeCondition))
        is GQLInlineFragment -> {
          if (typeSet.contains(it.typeCondition.name)) {
            collectFieldsInternal(it.selectionSet.selections, it.typeCondition.name, typeSet)
          } else {
            emptyList()
          }
        }
        is GQLFragmentSpread -> {
          val fragment = allGQLFragmentDefinitions[it.name]!!
          if (typeSet.contains(fragment.typeCondition.name)) {
            collectFieldsInternal(fragment.selectionSet.selections, fragment.typeCondition.name, typeSet)
          } else {
            emptyList()
          }
        }
      }
    }
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
    val fields = collectFieldsInternal(
        selections = selections,
        typeCondition = fieldType,
        typeSet = typeSet,
    ).let {
      fieldMerger.merge(it)
    }.map { mergedField ->
      val superFields = superFieldSets.mapNotNull {
        it.fields.firstOrNull { it.responseName == mergedField.info.responseName }
      }

      buildField(
          info = mergedField.info,
          condition = mergedField.condition,
          selections = mergedField.selections,
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
        syntheticFields = emptyList(),
        implements = implements,
        path = path,
    )
  }
}
