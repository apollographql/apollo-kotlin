package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.Catch
import com.apollographql.apollo.ast.CatchTo
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.lowerCamelCaseIgnoringNonLetters
import com.apollographql.apollo.compiler.codegen.modelName
import com.apollographql.apollo.compiler.decapitalizeFirstLetter
import com.apollographql.apollo.compiler.internal.escapeKotlinReservedWord

internal class OperationBasedModelGroupBuilder(
    private val schema: Schema,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldMerger: FieldMerger,
) : ModelGroupBuilder {

  override fun buildOperationData(selections: List<GQLSelection>, rawTypeName: String, operationName: String, defaultCatchTo: CatchTo?): Pair<IrProperty, IrModelGroup> {
    val info = IrFieldInfo(
        responseName = "data",
        description = null,
        type = IrModelType(MODEL_UNKNOWN),
        deprecationReason = null,
        optInFeature = null,
        gqlType = GQLNonNullType(type = GQLNamedType(name = rawTypeName)),
    )

    val field = buildField(
        path = "${MODEL_OPERATION_DATA}.$operationName",
        info = info,
        selections = selections,
        parentType = rawTypeName,
        condition = BooleanExpression.True,
        parentTypeConditions = listOf(rawTypeName),
        defaultCatchTo = defaultCatchTo,
    )

    return field.toProperty() to field.toModelGroup()!!
  }

  override fun buildFragmentInterface(fragmentName: String): IrModelGroup? {
    return null
  }

  override fun buildFragmentData(fragmentName: String, defaultCatchTo: CatchTo?): Pair<IrProperty, IrModelGroup> {
    val fragmentDefinition = allFragmentDefinitions[fragmentName]!!

    /**
     * XXX: because we want the model to be named after the fragment (and not data), we use
     * fragmentName below. This means the id for the very first model is going to be
     * FragmentName.FragmentName unlike operations where it's OperationName.Data
     */
    val info = IrFieldInfo(
        responseName = fragmentName,
        description = null,
        type = IrModelType(MODEL_UNKNOWN),
        deprecationReason = null,
        optInFeature = null,
        gqlType = GQLNonNullType(type = fragmentDefinition.typeCondition),
    )


    val mergedSelections = fragmentDefinition.selections

    val field = buildField(
        path = "${MODEL_FRAGMENT_DATA}.$fragmentName",
        info = info,
        selections = mergedSelections,
        parentType = fragmentDefinition.typeCondition.name,
        condition = BooleanExpression.True,
        parentTypeConditions = listOf(fragmentDefinition.typeCondition.name),
        defaultCatchTo = defaultCatchTo,
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

  /**
   * @param path the path up to but not including this field
   * @param info information about this field.
   * @param selections the sub-selections of this field.
   * @param parentType the parent type for [selections].
   * @param condition the condition for this field. Might be a mix of include directives and type conditions
   * @param parentTypeConditions the list of the different typeCondition going through all inline fragments
   */
  private fun buildField(
      path: String,
      info: IrFieldInfo,
      selections: List<GQLSelection>,
      parentType: String,
      condition: BooleanExpression<BTerm>,
      parentTypeConditions: List<String>,
      defaultCatchTo: CatchTo?,
  ): OperationField {
    if (selections.isEmpty()) {
      return OperationField(
          info = info,
          condition = condition,
          fieldSet = null,
      )
    }

    val selfPath = path + "." + info.responseName

    /**
     * Merge fragments with the same type condition and include directive to avoid name clashes
     *
     * We don't merge inline fragments with different @include directives as nested fields would all have to become nullable:
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
    val inlineFragmentsFields = selections.filterIsInstance<GQLInlineFragment>()
        .groupBy {
          it.typeCondition?.name ?: parentType
        }.entries.flatMap {
          val typeCondition = it.key

          val inlineFragmentsWithSameTypeCondition = it.value
          // If there is only one fragment, no need to disambiguate it
          /**
           * TODO: we could go one step further and check the condition too
           *
           * Right now this:
           * {
           *   ... on Cat @include(if: $foo) {
           *     #
           *   }
           *   ... on Cat @include(if: $foo) {
           *     #
           *   }
           * }
           *
           * Will create a `OnCatIfFoo` when `OnCat` would have been enough
           */
          val nameNeedsCondition = inlineFragmentsWithSameTypeCondition.size > 1

          /**
           * Because fragments are not merged, all inline fragments
           * should have the same parentType here
           */
          inlineFragmentsWithSameTypeCondition.groupBy { it.directives.toBooleanExpression() }
              .entries.map { entry ->
                val prefix = "on"

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

                var type: IrType = IrModelType(MODEL_UNKNOWN, nullable = true)
                if (childCondition == BooleanExpression.True) {
                  type = type.nullable(false)
                }

                val childInfo = IrFieldInfo(
                    responseName = "$prefix$name",
                    description = "Synthetic field for inline fragment on $typeCondition",
                    deprecationReason = null,
                    optInFeature = null,
                    type = type,
                    gqlType = null,
                )

                val childSelections = entry.value.flatMap {
                  it.selections
                }

                buildField(
                    path = selfPath,
                    info = childInfo,
                    selections = childSelections,
                    parentType = typeCondition,
                    condition = childCondition,
                    parentTypeConditions = parentTypeConditions + typeCondition,
                    defaultCatchTo = defaultCatchTo,
                )
              }
        }

    /**
     * Merge fragment spreads, regardless of the type condition
     *
     * Since they all have the same shape, it's ok, contrary to inline fragments above
     */
    val fragmentSpreadFields = selections.filterIsInstance<GQLFragmentSpread>()
        .groupBy {
          it.name
        }.values.map { fragmentSpreadsWithSameName ->
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

          var type: IrType = IrModelType(fragmentModelPath, nullable = true)
          if (childCondition == BooleanExpression.True) {
            type = type.nullable(false)
          }

          val childInfo = IrFieldInfo(
              responseName = first.name.decapitalizeFirstLetter().escapeKotlinReservedWord(),
              description = "Synthetic field for '${first.name}'",
              deprecationReason = null,
              optInFeature = null,
              type = type,
              gqlType = null,
          )

          buildField(
              path = selfPath,
              info = childInfo,
              selections = emptyList(), // Don't create a model for fragments spreads
              parentType = typeCondition, // unused
              condition = childCondition,
              parentTypeConditions = emptyList(),
              defaultCatchTo = defaultCatchTo // this is not used because this field has no sub selections
          )
        }

    val modelName = modelName(info)

    /**
     * Merge fields with the same response name in the selectionSet
     */
    val fieldsWithParent = selections.mapNotNull {
      if (it is GQLField) {
        FieldWithParent(it, parentType)
      } else {
        null
      }
    }
    val fields = fieldMerger.merge(fieldsWithParent, defaultCatchTo =  defaultCatchTo).map { mergedField ->
      val childInfo = mergedField.info.maybeNullable(mergedField.condition != BooleanExpression.True)

      /**
       * We always pass True here because the non-synthetic fields are always read. This simplifies the generated code
       * This means:
       * - `@defer` doesn't work on fields but this is ok because it is not supposed to
       * - we cannot detect if a field returns `null` whereas it should have been @skipped
       */
      val fieldCondition = BooleanExpression.True

      buildField(
          path = selfPath,
          info = childInfo,
          selections = mergedField.selections,
          parentType = mergedField.rawTypeName,
          condition = fieldCondition,
          parentTypeConditions = listOf(mergedField.rawTypeName),
          defaultCatchTo = defaultCatchTo
      )
    }

    val fieldSet = OperationFieldSet(
        id = selfPath,
        modelName = modelName,
        fields = fields + inlineFragmentsFields + fragmentSpreadFields,
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
    )
  }
}

private class OperationField(
    val info: IrFieldInfo,
    val condition: BooleanExpression<BTerm>,
    val fieldSet: OperationFieldSet?,
) {
  val isSynthetic: Boolean
    get() = info.gqlType == null

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

  val model = fieldSet.toModel()
  return IrModelGroup(
      models = listOf(model),
      baseModelId = model.id
  )
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
