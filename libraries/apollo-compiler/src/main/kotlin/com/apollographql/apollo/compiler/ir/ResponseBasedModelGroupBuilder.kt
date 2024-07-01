package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.CatchTo
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.compiler.codegen.modelName
import com.apollographql.apollo.compiler.upperCamelCaseIgnoringNonLetters

internal class ResponseBasedModelGroupBuilder(
    schema: Schema,
    allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    fieldMerger: FieldMerger,
) : ModelGroupBuilder {
  private val fieldNodeBuilder = FieldNodeBuilder(schema, allFragmentDefinitions, fieldMerger)

  override fun buildOperationData(
      selections: List<GQLSelection>,
      rawTypeName: String,
      operationName: String,
      defaultCatchTo: CatchTo?,
  ): Pair<IrProperty, IrModelGroup> {
    check(defaultCatchTo == null) {
      "Apollo: responseBased codegen does not support @catch"
    }

    val field = fieldNodeBuilder.buildOperationData(
        selections,
        rawTypeName,
        operationName
    )
    return field.toIrProperty() to field.toIrModelGroup()!!
  }

  override fun buildFragmentInterface(
      fragmentName: String,
  ): IrModelGroup {
    return fieldNodeBuilder.buildFragmentInterface(
        fragmentName
    ).toIrModelGroup()!!
  }

  override fun buildFragmentData(
      fragmentName: String,
      defaultCatchTo: CatchTo?,
  ): Pair<IrProperty, IrModelGroup> {
    check(defaultCatchTo == null) {
      "Apollo: responseBased codegen does not support @catch"
    }

    val field = fieldNodeBuilder.buildFragmentData(
        fragmentName
    )
    return field.toIrProperty() to field.toIrModelGroup()!!
  }
}

private data class ResponseField(
    val info: IrFieldInfo,
    val override: Boolean,
    val condition: BooleanExpression<BVariable>,
    val responseFieldSets: List<ResponseFieldSet>,
    val modelId: String?,
)

private data class ResponseFieldSet(
    val id: String,
    val typeSet: TypeSet,
    val responseFields: List<ResponseField>,
    val possibleTypes: Set<String>,
    val accessors: List<IrAccessor>,
    val implements: List<String>,
    val rawTypename: String,
    /**
     * Needed for ordering the type specs as well as naming the models
     */
    val isOther: Boolean,
    val isInterface: Boolean,
    val isFallback: Boolean,
)

/**
 * A comparator that sorts the [ResponseFieldSet] to make it easier to navigate the generated models
 * Sort by:
 * - Interfaces
 * - DataClasses
 * - Other
 */
private val FieldSetNodeComparator = Comparator<ResponseFieldSet> { a, b ->
  var r = a.isOther.compareTo(b.isOther)
  if (r != 0) {
    return@Comparator r
  }

  r = a.possibleTypes.isNotEmpty().compareTo(b.possibleTypes.isNotEmpty())
  if (r != 0) {
    return@Comparator r
  }

  return@Comparator a.typeSet.size - b.typeSet.size
}

private fun subpath(path: String, info: IrFieldInfo, typeSet: TypeSet, isOther: Boolean): String {
  val name = upperCamelCaseIgnoringNonLetters(typeSet.sorted() + info.responseName)

  return path + "." + (if (isOther) "Other" else "") + name
}

private class FieldNodeBuilder(
    private val schema: Schema,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldMerger: FieldMerger,
) {
  private var cachedFragmentFieldNodes = mutableMapOf<String, ResponseField>()

  private fun getSuperFieldSetNodes(typeSet: TypeSet, candidates: Collection<ResponseFieldSet>): List<ResponseFieldSet> {
    val superTypeSets = superTypeSets(typeSet, candidates.map { it.typeSet })
    return candidates.filter {
      superTypeSets.contains(it.typeSet)
    }
  }

  fun buildOperationData(
      selections: List<GQLSelection>,
      rawTypeName: String,
      operationName: String,
  ): ResponseField {
    val info = IrFieldInfo(
        responseName = "data",
        description = null,
        type = IrModelType(MODEL_UNKNOWN),
        deprecationReason = null,
        optInFeature = null,
        gqlType = GQLNonNullType(type = GQLNamedType(name = rawTypeName)),
    )

    return buildFieldNode(
        modelPath = "${MODEL_OPERATION_DATA}.$operationName",
        info = info,
        selections = selections,
        rawTypename = rawTypeName,
        condition = BooleanExpression.True,
        superResponseFields = emptyList(),
        withImplementations = true
    )
  }

  fun buildFragmentInterface(name: String): ResponseField {
    return cachedFragmentFieldNodes.getOrPut(name) {
      val fragment = allFragmentDefinitions[name] ?: error("Cannot find fragment $name")

      val info = IrFieldInfo(
          responseName = name,
          description = null,
          type = IrModelType(MODEL_UNKNOWN, nullable = true),
          deprecationReason = null,
          optInFeature = null,
          gqlType = GQLNonNullType(type = fragment.typeCondition),
      )

      return buildFieldNode(
          modelPath = "${MODEL_FRAGMENT_INTERFACE}.$name",
          info = info,
          selections = fragment.selections,
          rawTypename = fragment.typeCondition.name,
          condition = BooleanExpression.True,
          superResponseFields = emptyList(),
          withImplementations = false
      )
    }
  }

  fun buildFragmentData(name: String): ResponseField {
    val ifaceFieldNode = buildFragmentInterface(name)
    val fragment = allFragmentDefinitions[name] ?: error("Cannot find fragment $name")

    val info = IrFieldInfo(
        responseName = "data",
        description = null,
        type = IrModelType(MODEL_UNKNOWN),
        deprecationReason = null,
        optInFeature = null,
        gqlType = GQLNonNullType(type = fragment.typeCondition),
    )

    return buildFieldNode(
        modelPath = "${MODEL_FRAGMENT_DATA}.$name",
        info = info,
        selections = fragment.selections,
        rawTypename = fragment.typeCondition.name,
        condition = BooleanExpression.True,
        superResponseFields = listOf(ifaceFieldNode),
        withImplementations = true
    )
  }

  private class FieldState(
      val superResponseFields: List<ResponseField>,
      val fragmentResponseFields: List<ResponseField>,
      val info: IrFieldInfo,
      val modelDescriptors: Set<ModelDescriptor>,
      val path: String,
      val selections: List<GQLSelection>,
      val rawTypename: String,
  ) {
    val cachedFieldSetNodes = mutableMapOf<ModelDescriptor, ResponseFieldSet>()
  }

  private fun getModelId(models: Collection<ResponseFieldSet>, typeSet: TypeSet): String {
    return models.filter { it.typeSet == typeSet }.let {
      val ret = it.firstOrNull { it.isInterface } ?: it.firstOrNull()
      check(ret != null) {
        "Cannot find base model"
      }
      ret
    }.id
  }

  /**
   * Input params to build a model
   */
  private data class ModelDescriptor(
      val shape: Shape,
      val isOther: Boolean,
      val isInterface: Boolean,
  )

  /**
   * This builds the models greedily so that:
   * 1. we have a qualifiedName when needed
   * 2. we can build the "Other" fields
   *
   * @param modelPath the path of the model containing this field
   * @param info the field info. For composite types, [info].type contains a placeholder value that is replaced
   * once all field shapes have been built
   */
  private fun buildFieldNode(
      modelPath: String,
      info: IrFieldInfo,
      condition: BooleanExpression<BVariable>,
      selections: List<GQLSelection>,
      rawTypename: String,
      superResponseFields: List<ResponseField>,
      withImplementations: Boolean,
  ): ResponseField {
    if (selections.isEmpty()) {
      // fast path for non-compound types
      return ResponseField(
          info = info,
          override = superResponseFields.isNotEmpty(),
          condition = condition,
          responseFieldSets = emptyList(),
          modelId = null
      )
    }

    val shapes = shapes(schema, allFragmentDefinitions, selections, rawTypename)
    val allTypeSets = shapes.map { it.typeSet }.toSet()

    val fragmentFieldNodes = mutableListOf<ResponseField>()
    val fragmentAccessors = mutableListOf<IrFragmentAccessor>()

    /**
     * Build the fragments
     */
    selections.collectFragments().forEach {
      val fragmentFieldNode = buildFragmentInterface(it)
      fragmentFieldNodes.add(fragmentFieldNode)
      fragmentAccessors.add(
          IrFragmentAccessor(
              fragmentName = it,
              returnedModelId = fragmentFieldNode.modelId!!
          )
      )
    }

    val modelDescriptors = shapes.modelDescriptors(withImplementations, allTypeSets)

    val fieldState = FieldState(
        superResponseFields = superResponseFields,
        fragmentResponseFields = fragmentFieldNodes,
        info = info,
        modelDescriptors = modelDescriptors.toSet(),
        path = modelPath,
        selections = selections,
        rawTypename = rawTypename
    )

    val fieldSetNodes = modelDescriptors.map {
      buildFieldSetNode(
          fieldState,
          it,
      )
    }.sortedWith(FieldSetNodeComparator)

    /**
     * Patch the field with the type of the base model
     */
    val baseModelId = fieldSetNodes.first { it.typeSet.size == 1 }.id
    val patchedInfo = info.copy(type = info.type.replacePlaceholder(baseModelId))

    /**
     * Patch the base fieldSet with the accessors
     */
    val patchedFieldSetNodes = fieldSetNodes.map {
      if (it.id == baseModelId) {
        val subtypeAccessors = allTypeSets
            .filter { typeSet ->
              typeSet.size > 1
            }.map { typeSet ->
              IrSubtypeAccessor(
                  returnedModelId = getModelId(fieldSetNodes, typeSet),
                  typeSet = typeSet - rawTypename
              )
            }
        it.copy(accessors = fragmentAccessors + subtypeAccessors)
      } else {
        it
      }
    }

    return ResponseField(
        info = patchedInfo,
        override = superResponseFields.isNotEmpty(),
        condition = condition,
        responseFieldSets = patchedFieldSetNodes,
        modelId = baseModelId
    )
  }

  private fun buildFieldSetNode(
      state: FieldState,
      modelDescriptor: ModelDescriptor,
  ): ResponseFieldSet {
    val cached = state.cachedFieldSetNodes.get(modelDescriptor)
    if (cached != null) {
      return cached
    }

    val typeSet = modelDescriptor.shape.typeSet
    val isOther = modelDescriptor.isOther
    val isInterface = modelDescriptor.isInterface
    val allTypeSets = state.modelDescriptors.map { it.shape.typeSet }.toSet()

    val superTypeSets = if (isOther) {
      setOf(typeSet)
    } else {
      strictlySuperTypeSets(typeSet, allTypeSets)
    }

    val superSelfFieldSetNodes = superTypeSets.map { superTypeSet ->
      val descriptor = state.modelDescriptors.first {
        it.shape.typeSet == superTypeSet && it.isInterface
      }
      buildFieldSetNode(state, descriptor)
    }

    val superFragmentFieldSetNodes = state.fragmentResponseFields.flatMap { fieldNode ->
      getSuperFieldSetNodes(typeSet, fieldNode.responseFieldSets)
    }
    val superSiblingFieldSetNodes = state.superResponseFields.flatMap { fieldNode ->
      getSuperFieldSetNodes(typeSet, fieldNode.responseFieldSets)
    }

    val implementedFieldSetNodes = (superSelfFieldSetNodes + superFragmentFieldSetNodes + superSiblingFieldSetNodes)

    val mergedFields = fieldMerger.merge(
        collectFields(
            allFragmentDefinitions = allFragmentDefinitions,
            selections = state.selections,
            parentTypeDefinition = state.rawTypename,
            typeSet = typeSet,
        ),
        null
    )

    val path = subpath(state.path, state.info, typeSet, isOther)
    val fieldSetNode = ResponseFieldSet(
        id = path,
        accessors = emptyList(),
        responseFields = mergedFields.map { mergedField ->
          buildFieldNode(
              modelPath = path,
              info = mergedField.info,
              condition = mergedField.condition,
              selections = mergedField.selections,
              rawTypename = mergedField.rawTypeName,
              superResponseFields = implementedFieldSetNodes.flatMap {
                it.responseFields.filter { it.info.responseName == mergedField.info.responseName }
              },
              withImplementations = !isInterface
          )
        },
        possibleTypes = modelDescriptor.shape.possibleTypes,
        typeSet = typeSet,
        implements = implementedFieldSetNodes.map { it.id },
        isOther = isOther,
        isInterface = isInterface,
        isFallback = typeSet.size == 1 && isOther,
        rawTypename = state.rawTypename
    )

    state.cachedFieldSetNodes.put(modelDescriptor, fieldSetNode)

    return fieldSetNode
  }

  /**
   * For a list of shapes, returns a list of [ModelDescriptor] that indicate whether to build the shape as interface,
   * as class or both
   */
  private fun List<Shape>.modelDescriptors(withImplementations: Boolean, allTypeSets: Set<TypeSet>): List<ModelDescriptor> {
    return if (withImplementations) {
      flatMap {
        when {
          it.possibleTypes.isEmpty() && it.typeSet.size > 1 -> {
            /**
             * Interface-Only
             *
             * This shape has no possible types and is not the base shape.
             * For an example:
             * User typesets: [A, B], [A, C], [A, D]
             * Shapes: [A, B, C], [A, B, D]
             *
             * [A, B] is an interface
             *
             * The base shape is special because if the field is not monomorphic, we always want the "other" model to be generated.
             */
            listOf(
                ModelDescriptor(
                    shape = it,
                    isInterface = true,
                    isOther = false,
                )
            )
          }
          subTypeCount(it.typeSet, allTypeSets) == 0 -> {
            /**
             * Class-Only
             *
             * This shape has no descendants. By construction, it should be a class.
             *
             * For monomorphic fields, the base shape is generated as a class.
             */
            listOf(
                ModelDescriptor(
                    shape = it,
                    isInterface = false,
                    isOther = false,
                )
            )
          }
          else -> {
            /**
             * Both Interface and Class
             *
             * possibleTypes is not empty and this shape has descendants.
             *
             * For an example:
             * User typesets: [A, B], [A, C]
             * Shapes: [A, B], [A, B, C]
             *
             * [A, B] is an interface and a class
             */
            listOf(
                ModelDescriptor(
                    shape = it,
                    isInterface = true,
                    isOther = false,
                ),
                ModelDescriptor(
                    shape = it,
                    isInterface = false,
                    isOther = true,
                )
            )
          }
        }
      }
    } else {
      /**
       * Generating without the implementations is easy, always generate as interfaces
       */
      map {
        ModelDescriptor(
            shape = it,
            isInterface = true,
            isOther = false,
        )
      }
    }
  }
}


private fun ResponseField.toIrModelGroup(): IrModelGroup? {
  if (responseFieldSets.isEmpty()) {
    return null
  }
  return IrModelGroup(
      baseModelId = modelId!!,
      models = responseFieldSets.map { it.toIrModel(this) }
  )
}

private fun ResponseFieldSet.toIrModel(parentResponseField: ResponseField): IrModel {
  return IrModel(
      modelName = modelName(parentResponseField.info, typeSet, rawTypename, isOther),
      possibleTypes = possibleTypes.toList(),
      modelGroups = responseFields.mapNotNull { it.toIrModelGroup() },
      properties = responseFields.map { it.toIrProperty() },
      implements = implements,
      accessors = accessors,
      id = id,
      typeSet = typeSet,
      isInterface = isInterface,
      isFallback = isFallback,
  )
}

private fun ResponseField.toIrProperty(): IrProperty {
  var type = info.type
  if (condition != BooleanExpression.True) {
    type = type.nullable(true)
  }
  return IrProperty(
      info = info.copy(type = type),
      override = override,
      condition = BooleanExpression.True,
      requiresBuffering = false,
  )
}

private fun List<GQLSelection>.collectFragments(): Set<String> {
  return flatMap {
    when (it) {
      is GQLField -> emptySet()
      is GQLInlineFragment -> {
        it.selections.collectFragments()
      }
      // We do not recurse here as inheriting the first namedFragment will
      // inherit nested ones as well
      is GQLFragmentSpread -> return@flatMap setOf(it.name)
    }
  }.toSet()
}
