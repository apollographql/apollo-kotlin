package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.api.BLabel
import com.apollographql.apollo3.api.BPossibleTypes
import com.apollographql.apollo3.api.BTerm
import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.api.and
import com.apollographql.apollo3.api.firstElementOfType
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.transformation.mergeTrivialInlineFragments
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.codegen.CodegenLayout.Companion.lowerCamelCaseIgnoringNonLetters
import com.apollographql.apollo3.compiler.codegen.CodegenLayout.Companion.modelName
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord

internal class OperationBasedModelGroupBuilder(
    private val schema: Schema,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldMerger: FieldMerger,
    private val compat: Boolean,
    private val operationBased2: Boolean
) : ModelGroupBuilder {
  private val insertFragmentSyntheticField = compat
  private val collectAllInlineFragmentFields = compat
  private val mergeTrivialInlineFragments = compat

  /**
   * This is required for compatibility with v2. There might be ways to isolate that algorithm outside of [OperationBasedModelGroupBuilder]
   *
   * Note: we do not handle the case where a fragment has the same name as a field
   */
  private fun resolveNameClashes(usedNames: MutableSet<String>, modelName: String): String {
    if (!compat) {
      return modelName
    }

    var i = 0
    var name = modelName
    while (usedNames.contains(name)) {
      i++
      name = "$modelName$i"
    }
    usedNames.add(name)
    return name
  }

  override fun buildOperationData(selections: List<GQLSelection>, rawTypeName: String, operationName: String): Pair<IrProperty, IrModelGroup> {
    val info = IrFieldInfo(
        responseName = "data",
        description = null,
        type = IrNonNullType(IrModelType(MODEL_UNKNOWN)),
        deprecationReason = null,
        optInFeature = null,
        gqlType = GQLNonNullType(type = GQLNamedType(name = rawTypeName)),
    )

    val mergedSelections = if (mergeTrivialInlineFragments) {
      selections.mergeTrivialInlineFragments(schema, rawTypeName)
    } else {
      selections
    }
    val usedNames = mutableSetOf<String>()
    val field = buildField(
        path = "${MODEL_OPERATION_DATA}.$operationName",
        info = info,
        selections = mergedSelections.map { SelectionWithParent(it, rawTypeName) },
        condition = BooleanExpression.True,
        usedNames = usedNames,
        parentTypeConditions = listOf(rawTypeName),
    )

    return field.toProperty() to field.toModelGroup()!!
  }

  override fun buildFragmentInterface(fragmentName: String): IrModelGroup? {
    return null
  }

  override fun buildFragmentData(fragmentName: String): Pair<IrProperty, IrModelGroup> {
    val fragmentDefinition = allFragmentDefinitions[fragmentName]!!

    /**
     * XXX: because we want the model to be named after the fragment (and not data), we use
     * fragmentName below. This means the id for the very first model is going to be
     * FragmentName.FragmentName unlike operations where it's OperationName.Data
     */
    val info = IrFieldInfo(
        responseName = fragmentName,
        description = null,
        type = IrNonNullType(IrModelType(MODEL_UNKNOWN)),
        deprecationReason = null,
        optInFeature = null,
        gqlType = GQLNonNullType(type = fragmentDefinition.typeCondition),
    )


    val mergedSelections = if (mergeTrivialInlineFragments) {
      fragmentDefinition.selectionSet.selections.mergeTrivialInlineFragments(schema, fragmentDefinition.typeCondition.name)
    } else {
      fragmentDefinition.selectionSet.selections
    }

    val usedNames = mutableSetOf<String>()

    val field = buildField(
        path = "${MODEL_FRAGMENT_DATA}.$fragmentName",
        info = info,
        selections = mergedSelections.map { SelectionWithParent(it, fragmentDefinition.typeCondition.name) },
        condition = BooleanExpression.True,
        usedNames = usedNames,
        parentTypeConditions = listOf(fragmentDefinition.typeCondition.name),
    )

    return field.toProperty() to field.toModelGroup()!!
  }

  /**
   * A grouping key for fragments
   */
  private data class InlineFragmentKey(val typeCondition: String, val condition: BooleanExpression<BTerm>)

  private fun BooleanExpression<BTerm>.toName(): String = when (this) {
    is BooleanExpression.True -> "True"
    is BooleanExpression.False -> "False"
    is BooleanExpression.And -> this.operands.joinToString("And") { it.toName() }
    is BooleanExpression.Or -> this.operands.joinToString("Or") { it.toName() }
    is BooleanExpression.Not -> "Not${this.operand.toName()}"
    is BooleanExpression.Element -> {
      val bVariable = this.value as? BVariable ?: error("Unexpected term type in toName")
      bVariable.name.capitalizeFirstLetter()
    }
    else -> error("Unexpected boolean expression type in toName")
  }

  private fun InlineFragmentKey.toName(): String = buildString {
    append(typeCondition.capitalizeFirstLetter())
    if (condition != BooleanExpression.True) {
      // If at least one condition is a BLabel, it is enough to make a unique name, because the label is unique
      val deferCondition: BLabel? = condition.firstElementOfType(BLabel::class)
      if (deferCondition != null) {
        append("Defer")
        deferCondition.label?.let { append(it.capitalizeFirstLetter()) }
      } else {
        append("If")
        append((condition).toName())
      }
    }
  }

  private class SelectionWithParent(val selection: GQLSelection, val parent: String)

  /**
   * @param path the path up to but not including this field
   * @param info information about this field
   * @param selections the sub-selections of these fields. If [collectAllInlineFragmentFields] is true, might contain parent fields that
   * might not all be on the same parent type. Hence [SelectionWithParent]
   * @param condition the condition for this field. Might be a mix of include directives and type conditions
   * @param usedNames the used names for 2.x compat name conflicts resolution
   * @param parentTypeConditions the list of the different typeCondition going through all inline fragments
   */
  private fun buildField(
      path: String,
      info: IrFieldInfo,
      selections: List<SelectionWithParent>,
      condition: BooleanExpression<BTerm>,
      usedNames: MutableSet<String>,
      parentTypeConditions: List<String>,
  ): OperationField {
    val rawTypeName = parentTypeConditions.lastOrNull() ?: ""
    if (selections.isEmpty()) {
      return OperationField(
          info = info,
          condition = condition,
          fieldSet = null,
          shapes = emptyList(),
          rawTypeName = rawTypeName
      )
    }

    val selfPath = path + "." + info.responseName
    val shapes = if (operationBased2) {
      shapes(
          schema,
          allFragmentDefinitions,
          selections.map { it.selection },
          rawTypeName,
          true
      ).filter { it.possibleTypes.isNotEmpty() }
    } else {
      emptyList()
    }

    /**
     * Merge fragments with the same type condition and include directive to avoid name clashes
     *
     * We don't merge inline fragments with different include directives as nested fields would all have to become nullable:
     *
     * ```
     * {
     *   ... on Droid @include(if: $a) {
     *     primaryFunction
     *     friend {
     *       firstName
     *     }
     *   }
     *   ... on Droid @include(if: $b) {
     *     friend {
     *       lastName
     *     }
     *   }
     * }
     * ```
     *
     * Merging these would yield
     *
     * ```
     * class onDroid(val primaryFunction: String?, val friend: Friend)
     * class Friend(val firstName: String?, val lastName: String?)
     * ```
     *
     * While this is technically doable, it goes against mapping to the operation 1:1 and also makes invalid states representable
     * (for an example both firstName = null and lastName = null)
     *
     */
    val inlineFragmentsFields = selections.filter { it.selection is GQLInlineFragment }
        .groupBy {
          (it.selection as GQLInlineFragment).typeCondition.name
        }.entries.flatMap {
          val typeCondition = it.key

          // If there is only one fragment, no need to disambiguate it
          val nameNeedsCondition = it.value.size > 1
          val inlineFragmentsWithSameTypeCondition = it.value.map { it.selection as GQLInlineFragment }

          /**
           * Because fragments are not merged regardless of [collectAllInlineFragmentFields], all inline fragments
           * should have the same parentType here
           */
          inlineFragmentsWithSameTypeCondition.groupBy { it.directives.toBooleanExpression() }
              .entries.map { entry ->
                val prefix = if (collectAllInlineFragmentFields) "as" else "on"

                val name = if (nameNeedsCondition) {
                  InlineFragmentKey(typeCondition, entry.key).toName()
                } else {
                  InlineFragmentKey(typeCondition, BooleanExpression.True).toName()
                }

                var childCondition: BooleanExpression<BTerm> = if (parentTypeConditions.any { schema.isTypeASubTypeOf(it, typeCondition) }) {
                  /**
                   * If any of the parent types is a subtype (e.g. Cat is a subtype of Animal) then we can skip checking the typename
                   */
                  BooleanExpression.True
                } else {
                  val possibleTypes = schema.possibleTypes(typeCondition)
                  BooleanExpression.Element(BPossibleTypes(possibleTypes))
                }
                childCondition = entry.key.and(childCondition).simplify()

                var type: IrType = IrModelType(MODEL_UNKNOWN)
                if (childCondition == BooleanExpression.True) {
                  type = IrNonNullType(type)
                }

                val childInfo = IrFieldInfo(
                    responseName = "$prefix$name",
                    description = "Synthetic field for inline fragment on $typeCondition",
                    deprecationReason = null,
                    optInFeature = null,
                    type = type,
                    gqlType = null,
                )

                var childSelections = entry.value.flatMap {
                  it.selectionSet.selections.map { SelectionWithParent(it, typeCondition) }
                }

                if (collectAllInlineFragmentFields) {
                  childSelections = selections.filter { it.selection is GQLField } + childSelections
                }

                buildField(
                    path = selfPath,
                    info = childInfo,
                    selections = childSelections,
                    condition = childCondition,
                    usedNames = usedNames,
                    parentTypeConditions = parentTypeConditions + typeCondition,
                )
              }
        }

    /**
     * Merge fragment spreads, regardless of the type condition
     *
     * Since they all have the same shape, it's ok, contrary to inline fragments above
     */
    val fragmentSpreadFields = selections.filter { it.selection is GQLFragmentSpread }
        .groupBy {
          (it.selection as GQLFragmentSpread).name
        }.values.map { values ->
          val fragmentSpreadsWithSameName = values.map { it.selection as GQLFragmentSpread }
          val first = fragmentSpreadsWithSameName.first()

          val fragmentDefinition = allFragmentDefinitions[first.name]!!
          val typeCondition = fragmentDefinition.typeCondition.name

          var childCondition: BooleanExpression<BTerm> = if (parentTypeConditions.any { schema.isTypeASubTypeOf(it, typeCondition) }) {
            /**
             * If any of the parent types is a subtype (e.g. Cat is a subtype of Animal) then we can skip checking the typename
             */
            BooleanExpression.True
          } else {
            val possibleTypes = schema.possibleTypes(typeCondition)
            BooleanExpression.Element(BPossibleTypes(possibleTypes))
          }

          /**
           * That's more involved than the inline fragment case because fragment spreads have different @include/@skip directives get merged together
           */
          childCondition = BooleanExpression.Or(fragmentSpreadsWithSameName.map { it.directives.toBooleanExpression() }.toSet())
              .simplify()
              .and(childCondition)
              .simplify()

          val fragmentModelPath = "${MODEL_FRAGMENT_DATA}.${first.name}.${first.name}"

          var type: IrType = IrModelType(fragmentModelPath)
          if (childCondition == BooleanExpression.True) {
            type = IrNonNullType(type)
          }

          val fragmentShapes = if (operationBased2) {
            shapes(
                schema,
                allFragmentDefinitions,
                fragmentDefinition.selectionSet.selections,
                fragmentDefinition.typeCondition.name,
                true
            ).filter { it.possibleTypes.isNotEmpty() }
          } else {
            emptyList()
          }

          val childInfo = IrFieldInfo(
              responseName = first.name.decapitalizeFirstLetter().escapeKotlinReservedWord(),
              description = "Synthetic field for '${first.name}'",
              deprecationReason = null,
              optInFeature = null,
              type = type,
              gqlType = null,
          )
          OperationField(
              info = childInfo,
              condition = childCondition,
              fieldSet = null,
              shapes = fragmentShapes,
              rawTypeName = typeCondition
          )
        }

    /**
     * Add the "Fragments" synthetic field for compat codegen
     */
    val fragmentsFields = if (insertFragmentSyntheticField && fragmentSpreadFields.isNotEmpty()) {
      val childPath = "$selfPath.$FRAGMENTS_SYNTHETIC_FIELD"

      val fragmentsFieldInfo = IrFieldInfo(
          responseName = FRAGMENTS_SYNTHETIC_FIELD,
          description = "Synthetic field for grouping fragments",
          deprecationReason = null,
          optInFeature = null,
          type = IrNonNullType(IrModelType(childPath)),
          gqlType = null,
      )

      val fragmentsFieldSet = OperationFieldSet(
          id = childPath,
          // No need to resolve the nameclashes here, "Fragments" are never flattened
          modelName = modelName(fragmentsFieldInfo),
          fields = fragmentSpreadFields,
      )

      listOf(
          OperationField(
              info = fragmentsFieldInfo,
              condition = BooleanExpression.True,
              fieldSet = fragmentsFieldSet,
              shapes = emptyList(),
              rawTypeName
          )
      )
    } else {
      fragmentSpreadFields
    }

    val modelName = resolveNameClashes(usedNames, modelName(info))

    /**
     * Merge fields with the same response name in the selectionSet
     * This comes last so that it mimics the 2.x behaviour of nameclash resolution
     */
    val fieldsWithParent = selections.mapNotNull {
      if (it.selection is GQLField) {
        FieldWithParent(it.selection, it.parent)
      } else {
        null
      }
    }
    val fields = fieldMerger.merge(fieldsWithParent).map { mergedField ->
      val childInfo = mergedField.info.maybeNullable(mergedField.condition != BooleanExpression.True)

      buildField(
          path = selfPath,
          info = childInfo,
          selections = mergedField.selections.map { SelectionWithParent(it, mergedField.rawTypeName) },
          condition = BooleanExpression.True,
          usedNames = usedNames,
          parentTypeConditions = listOf(mergedField.rawTypeName)
      )
    }

    val fieldSet = OperationFieldSet(
        id = selfPath,
        modelName = modelName,
        fields = fields + inlineFragmentsFields + fragmentsFields,
    )

    val patchedInfo = info.copy(
        type = info.type.replacePlaceholder(fieldSet.id),
        responseName = if (info.gqlType == null) {
          lowerCamelCaseIgnoringNonLetters(setOf(modelName))
        } else {
          info.responseName
        }
    )

    return OperationField(
        info = patchedInfo,
        condition = condition,
        fieldSet = fieldSet,
        shapes = shapes,
        rawTypeName = rawTypeName
    )
  }

  companion object {
    const val FRAGMENTS_SYNTHETIC_FIELD = "fragments"
  }
}

private class OperationField(
    val info: IrFieldInfo,
    val condition: BooleanExpression<BTerm>,
    val fieldSet: OperationFieldSet?,
    val shapes: List<Shape>,
    val rawTypeName: String
) {
  val isSynthetic: Boolean
    get() = info.gqlType == null
  val hasMultipleShapes: Boolean
    get() = shapes.size > 1
}

private data class OperationFieldSet(
    val id: String,
    val modelName: String,
    val fields: List<OperationField>,
)

private fun OperationField.toModelGroup(): IrModelGroup? {
  if (fieldSet == null) {
    return null
  }

  return if (hasMultipleShapes) {
    fieldSet.toShapedModels(shapes, rawTypeName)
  } else {
    val model = fieldSet.toModel()
    IrModelGroup(
        models = listOf(model),
        baseModelId = model.id,
        sharedModelGroups = emptyList()
    )
  }
}

private fun OperationFieldSet.toModel(): IrModel {
  return IrModel(
      modelName = modelName,
      id = id,
      properties = fields.map { it.toProperty() },
      accessors = emptyList(),
      implements = emptyList(),
      isFallback = false,
      isInterface = false,
      modelGroups = fields.mapNotNull { it.toModelGroup() },
      possibleTypes = emptyList(),
      typeSet = emptySet(),
  )
}

private fun OperationField.toProperty(): IrProperty {
  return IrProperty(
      info = info,
      override = false,
      condition = condition,
      requiresBuffering = fieldSet?.fields?.any { it.isSynthetic } ?: false,
  )
}

private fun OperationFieldSet.toShapedModels(
    shapes: List<Shape>,
    rawTypeName: String
): IrModelGroup {
  val shapedTypeModels = shapes
      .filter { it.typeSet.size > 1 }
      .map { toShapedTypeModel(it, rawTypeName) }
  val interfaceModel = toInterfaceModel()
  val modelList = listOf(interfaceModel) + shapedTypeModels + toFallbackModel()
  return IrModelGroup(
      baseModelId = interfaceModel.id,
      models = modelList,
      sharedModelGroups = interfaceModel.modelGroups
  )
}

private fun OperationFieldSet.toFallbackModel(): IrModel {
  return IrModel(
      modelName = makeFallbackModelName(modelName),
      id = makeFallbackModelID(id),
      properties = fields.map { it.toProperty().copy(override = true) },
      accessors = emptyList(),
      implements = listOf(id),
      isFallback = true,
      isInterface = false,
      modelGroups = emptyList(),
      possibleTypes = emptyList(),
      typeSet = emptySet(),
  )
}

private fun OperationFieldSet.toInterfaceModel(): IrModel {
  return IrModel(
      modelName = modelName,
      id = id,
      properties = fields.map { it.toProperty() },
      accessors = emptyList(),
      implements = emptyList(),
      isFallback = false,
      isInterface = true,
      modelGroups = fields.mapNotNull { it.toModelGroup() },
      possibleTypes = emptyList(),
      typeSet = emptySet(),
  )
}

private fun OperationFieldSet.toShapedTypeModel(shape: Shape, rawTypeName: String): IrModel {
  return IrModel(
      modelName = makeShapedModelName(modelName, shape, rawTypeName),
      id = makeShapedModelID(id, shape),
      properties = fields.map { it.toShapedProperty(shape) },
      accessors = emptyList(),
      implements = listOf(id),
      isFallback = false,
      isInterface = false,
      modelGroups = emptyList(),
      possibleTypes = shape.possibleTypes.toList(),
      typeSet = shape.typeSet,
  )
}

private fun OperationField.toShapedProperty(shape: Shape): IrProperty {
  val fieldShapedCondition = condition.simplifyBasedOnPossibleType(shape.possibleTypes)
  val fieldShapedType = if (fieldShapedCondition.simplify() == BooleanExpression.True) {
     info.type.makeNonNull()
  } else {
    info.type
  }
  return IrProperty(
      info = info.copy(type = fieldShapedType),
      override = true,
      condition = fieldShapedCondition,
      requiresBuffering = fieldSet?.fields?.any { it.isSynthetic } ?: false,
  )
}

private fun BPossibleTypes.isAlwaysSatisfied(possibleTypes: PossibleTypes): Boolean {
  return (possibleTypes - this.possibleTypes).isEmpty()
}

private fun BooleanExpression<BTerm>.simplifyBasedOnPossibleType(possibleTypes: PossibleTypes): BooleanExpression<BTerm> {
  val originalExpression = this
  return when(this) {
    BooleanExpression.True,
    BooleanExpression.False -> originalExpression
    is BooleanExpression.Not -> operand.simplifyBasedOnPossibleType(possibleTypes)
    is BooleanExpression.Or -> BooleanExpression.Or(operands.map { it.simplifyBasedOnPossibleType(possibleTypes) }.toSet())
    is BooleanExpression.And -> BooleanExpression.And(operands.map { it.simplifyBasedOnPossibleType(possibleTypes) }.toSet())
    is BooleanExpression.Element -> {
      when (value) {
        is BLabel,
        is BVariable -> originalExpression
        is BPossibleTypes -> {
          if ((value as BPossibleTypes).isAlwaysSatisfied(possibleTypes)) {
            BooleanExpression.True
          } else {
            originalExpression
          }
        }
      }
    }
  }
}

private fun makeShapedModelID(id: String, shape: Shape): String = "$id::${shape.modelIDSuffix}"
private fun makeFallbackModelID(id: String): String = "$id:::fallback"

private fun makeShapedModelName(name: String, shape: Shape, rawTypeName: String): String
  = "${shape.makeTypeSetPrefix(rawTypeName)}$name"
private fun makeFallbackModelName(name: String): String = "Other$name"


private fun Shape.makeTypeSetPrefix(rawTypeName: String) = CodegenLayout.upperCamelCaseIgnoringNonLetters(typeSet - rawTypeName)
private val Shape.modelIDSuffix get() = typeSet.sorted().joinToString("")

