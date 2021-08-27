package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.api.BPossibleTypes
import com.apollographql.apollo3.api.BTerm
import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.api.and
import com.apollographql.apollo3.compiler.codegen.CodegenLayout.Companion.modelName
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.transformation.mergeTrivialInlineFragments

internal class OperationBasedModelGroupBuilder(
    private val schema: Schema,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldMerger: FieldMerger,
    private val insertFragmentSyntheticField: Boolean,
    private val collectAllInlineFragmentFields: Boolean,
    private val mergeTrivialInlineFragments: Boolean
) : ModelGroupBuilder {

  override fun buildOperationData(selections: List<GQLSelection>, rawTypeName: String, operationName: String): IrModelGroup {
    val info = IrFieldInfo(
        responseName = "data",
        description = null,
        type = IrNonNullType(IrModelType(MODEL_UNKNOWN)),
        deprecationReason = null
    )

    val mergedSelections = if (mergeTrivialInlineFragments) {
      selections.mergeTrivialInlineFragments(schema, rawTypeName)
    } else {
      selections
    }
    return buildField(
        path = "${MODEL_OPERATION_DATA}.$operationName",
        info = info,
        selections = mergedSelections.map { SelectionWithParent(it, rawTypeName) },
        condition = BooleanExpression.True,
        isSynthetic = false
    ).toModelGroup()!!
  }

  override fun buildFragmentInterface(fragmentName: String): IrModelGroup? {
    return null
  }

  override fun buildFragmentData(fragmentName: String): IrModelGroup {
    /**
     * XXX: because we want the model to be named after the fragment (and not data), we use
     * fragmentName below. This means the id for the very first model is going to be
     * FragmentName.FragmentName unlike operations where it's OperationName.Data
     */
    val info = IrFieldInfo(
        responseName = fragmentName,
        description = null,
        type = IrNonNullType(IrModelType(MODEL_UNKNOWN)),
        deprecationReason = null
    )

    val fragmentDefinition = allFragmentDefinitions[fragmentName]!!

    val mergedSelections = if (mergeTrivialInlineFragments) {
      fragmentDefinition.selectionSet.selections.mergeTrivialInlineFragments(schema, fragmentDefinition.typeCondition.name)
    } else {
      fragmentDefinition.selectionSet.selections
    }

    return buildField(
        path = "${MODEL_FRAGMENT_DATA}.$fragmentName",
        info = info,
        selections = mergedSelections.map { SelectionWithParent(it, fragmentDefinition.typeCondition.name) },
        condition = BooleanExpression.True,
        isSynthetic = false
    ).toModelGroup()!!
  }

  /**
   * A grouping key for fragments
   */
  private data class InlineFragmentKey(val typeCondition: String, val condition: BooleanExpression<BVariable>)

  private fun BooleanExpression<BVariable>.toName(): String = when (this) {
    is BooleanExpression.True -> "True"
    is BooleanExpression.False -> "False"
    is BooleanExpression.And -> this.operands.joinToString("And") { it.toName() }
    is BooleanExpression.Or -> this.operands.joinToString("Or") { it.toName() }
    is BooleanExpression.Not -> "Not${this.operand.toName()}"
    is BooleanExpression.Element -> this.value.name.capitalize()
    else -> error("")
  }

  private fun InlineFragmentKey.toName(): String = buildString {
    append(typeCondition.capitalize())
    if (condition != BooleanExpression.True) {
      append("If")
      append(condition.toName())
    }
  }

  private class SelectionWithParent(val selection: GQLSelection, val parent: String)

  /**
   * @param kind the [IrKind] used to identify the resulting model
   * @param path the path up to but not including this field
   * @param info information about this field
   * @param selections the sub-selections of this fields. If [collectAllInlineFragmentFields] is true, might contain parent fields that
   * might not all be on the same parent type. Hence [SelectionWithParent]
   * @param condition the condition for this field. Might be a mix of include directives and type conditions
   * @param isSynthetic whether this is a synthetic field. This is used to determine whether this field requires buffering
   *
   */
  private fun buildField(
      path: String,
      info: IrFieldInfo,
      selections: List<SelectionWithParent>,
      condition: BooleanExpression<BTerm>,
      isSynthetic: Boolean,
  ): OperationField {
    if (selections.isEmpty()) {
      return OperationField(
          info = info,
          condition = condition,
          fieldSet = null,
          isSynthetic = isSynthetic,
          hide = false,
      )
    }

    val selfPath = path + "." + info.responseName

    /**
     * Merge fields with the same response name in the selectionSet
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
          isSynthetic = false
      )
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
          val parentTypeCondition = it.value.first().parent

          inlineFragmentsWithSameTypeCondition.groupBy { it.directives.toBooleanExpression() }
              .entries.map { entry ->
                val prefix = if (collectAllInlineFragmentFields) "as" else "on"

                val name = if (nameNeedsCondition) {
                  InlineFragmentKey(typeCondition, entry.key).toName()
                } else {
                  InlineFragmentKey(typeCondition, BooleanExpression.True).toName()
                }

                val possibleTypes = schema.possibleTypes(typeCondition)
                var childCondition: BooleanExpression<BTerm> = if (typeCondition == parentTypeCondition) {
                  BooleanExpression.True
                } else {
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
                    type = type
                )

                var childSelections = entry.value.flatMap {
                  it.selectionSet.selections.map { SelectionWithParent(it, typeCondition) }
                }

                if (collectAllInlineFragmentFields) {
                  childSelections = selections.filter { it.selection is GQLField} + childSelections
                }

                buildField(
                    path = selfPath,
                    info = childInfo,
                    selections = childSelections,
                    condition = childCondition,
                    isSynthetic = true
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

          /**
           * Because fragments are not merged regardless of [collectAllInlineFragmentFields], all inline fragments
           * should have the same parentType here
           */
          val parentTypeCondition = values.first().parent

          val possibleTypes = schema.possibleTypes(typeCondition)
          var childCondition: BooleanExpression<BTerm> = if (typeCondition != parentTypeCondition) {
            BooleanExpression.Element(BPossibleTypes(possibleTypes))
          } else {
            BooleanExpression.True
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

          val childInfo = IrFieldInfo(
              responseName = first.name.decapitalize().escapeKotlinReservedWord(),
              description = "Synthetic field for '${first.name}'",
              deprecationReason = null,
              type = type
          )

          val p = if (insertFragmentSyntheticField) {
            "$selfPath.$FRAGMENTS_SYNTHETIC_FIELD"
          } else {
            selfPath
          }
          buildField(
              path = p,
              info = childInfo,
              selections = emptyList(), // Don't create a model for fragments spreads
              condition = childCondition,
              isSynthetic = true
          )
        }

    val fragmentsFields = if (insertFragmentSyntheticField && fragmentSpreadFields.isNotEmpty()) {
      val childPath = "$selfPath.$FRAGMENTS_SYNTHETIC_FIELD"

      val fragmentsFieldSet = OperationFieldSet(
          id = childPath,
          fields = listOf(hiddenTypenameField) + fragmentSpreadFields
      )

      val fragmentsFieldInfo = IrFieldInfo(
          responseName = FRAGMENTS_SYNTHETIC_FIELD,
          description = "Synthetic field for grouping fragments",
          deprecationReason = null,
          type = IrNonNullType(IrModelType(fragmentsFieldSet.id))
      )

      listOf(
          OperationField(
              info = fragmentsFieldInfo,
              condition = BooleanExpression.True,
              fieldSet = fragmentsFieldSet,
              isSynthetic = true,
              hide = false
          )
      )
    } else {
      fragmentSpreadFields
    }
    val fieldSet = OperationFieldSet(
        id = selfPath,
        fields = fields + inlineFragmentsFields + fragmentsFields
    )

    val patchedInfo = info.copy(type = info.type.replacePlaceholder(fieldSet.id))

    return OperationField(
        info = patchedInfo,
        condition = condition,
        fieldSet = fieldSet,
        isSynthetic = isSynthetic,
        hide = false
    )
  }

  companion object {
    const val FRAGMENTS_SYNTHETIC_FIELD = "fragments"

    private val hiddenTypenameField by lazy {
      val info = IrFieldInfo(
          responseName = "__typename",
          description = null,
          deprecationReason = null,
          type = IrNonNullType(IrStringType)
      )
      OperationField(
          info = info,
          condition = BooleanExpression.True,
          fieldSet = null,
          isSynthetic = false,
          hide = true
      )
    }

  }
}

private class OperationField(
    val info: IrFieldInfo,
    val condition: BooleanExpression<BTerm>,
    val fieldSet: OperationFieldSet?,
    val isSynthetic: Boolean,
    val hide: Boolean,
)

private data class OperationFieldSet(
    val id: String,
    val fields: List<OperationField>,
)

private fun OperationField.toModelGroup(): IrModelGroup? {
  if (fieldSet == null) {
    return null
  }

  val model = fieldSet.toModel(info)
  return IrModelGroup(
      models = listOf(model),
      baseModelId = model.id
  )
}

private fun OperationFieldSet.toModel(info: IrFieldInfo): IrModel {
  return IrModel(
      modelName = modelName(info),
      id = id,
      properties = fields.map { it.toProperty() },
      accessors = emptyList(),
      implements = emptyList(),
      isFallback = false,
      isInterface = false,
      modelGroups = fields.mapNotNull { it.toModelGroup() },
      possibleTypes = emptySet(),
      typeSet = emptySet(),
  )
}

private fun OperationField.toProperty(): IrProperty {
  return IrProperty(
      info = info,
      isSynthetic = isSynthetic,
      override = false,
      condition = condition,
      requiresBuffering = fieldSet?.fields?.any { it.isSynthetic } ?: false,
      hidden = hide
  )
}