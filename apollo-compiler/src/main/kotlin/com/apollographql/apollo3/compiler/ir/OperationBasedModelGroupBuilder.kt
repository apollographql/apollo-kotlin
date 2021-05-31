package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.api.BPossibleTypes
import com.apollographql.apollo3.api.BTerm
import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.api.and
import com.apollographql.apollo3.compiler.codegen.CgLayout.Companion.modelName
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Schema

internal class OperationBasedModelGroupBuilder(
    private val schema: Schema,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldMerger: FieldMerger,
    private val insertFragmentSyntheticField: Boolean,
    private val collectAllInlineFragmentFields: Boolean,
) : ModelGroupBuilder {
  override fun buildOperationData(selections: List<GQLSelection>, rawTypeName: String, operationName: String): IrModelGroup {
    val root = IrModelRoot(
        IrRootKind.OperationData,
        operationName
    )
    val info = IrFieldInfo(
        responseName = "data",
        description = null,
        type = IrNonNullType(IrModelType(IrUnknownModelId)),
        deprecationReason = null
    )

    return buildField(
        root = root,
        path = "",
        info = info,
        selections = selections.map { SelectionWithParent(it, rawTypeName) },
        condition = BooleanExpression.True,
        isSynthetic = false
    ).toModelGroup()!!
  }

  override fun buildFragmentInterface(fragmentName: String): IrModelGroup? {
    return null
  }

  override fun buildFragmentData(fragmentName: String): IrModelGroup {
    val root = IrModelRoot(
        IrRootKind.FragmentData,
        fragmentName
    )
    val info = IrFieldInfo(
        responseName = fragmentName,
        description = null,
        type = IrNonNullType(IrModelType(IrUnknownModelId)),
        deprecationReason = null
    )

    val fragmentDefinition = allFragmentDefinitions[fragmentName]!!
    return buildField(
        root = root,
        path = "",
        info = info,
        selections = fragmentDefinition.selectionSet.selections.map { SelectionWithParent(it, fragmentDefinition.typeCondition.name) },
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
   * @param path the path up to but not including this field
   */
  private fun buildField(
      root: IrModelRoot,
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
        FieldWithParent(it.selection as GQLField, it.parent)
      } else {
        null
      }
    }
    val fields = fieldMerger.merge(fieldsWithParent).map { mergedField ->
      val childInfo = mergedField.info.maybeNullable(mergedField.condition != BooleanExpression.True)

      buildField(
          root = root,
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
    val inlineFragmentsFields = selections.map { it.selection }.filterIsInstance<GQLInlineFragment>()
        .groupBy {
          InlineFragmentKey(it.typeCondition.name, it.directives.toBooleanExpression())
        }.entries.map { entry ->
          val inlineFragmentsWithSameKey = entry.value
          val typeCondition = entry.key.typeCondition
          val prefix = if (collectAllInlineFragmentFields) "as" else "on"

          val childInfo = IrFieldInfo(
              responseName = "$prefix${entry.key.toName()}",
              description = "Synthetic field for inline fragment on $typeCondition",
              deprecationReason = null,
              type = IrModelType(IrUnknownModelId)
          )

          val possibleTypes = schema.possibleTypes(typeCondition)
          val childCondition = entry.key.condition.and(BooleanExpression.Element(BPossibleTypes(possibleTypes))).simplify()

          val childSelections = inlineFragmentsWithSameKey.flatMap {
            it.selectionSet.selections.map { SelectionWithParent(it, typeCondition) }
          }.toMutableList()
          if (collectAllInlineFragmentFields) {
            childSelections.addAll(selections.filter { it.selection is GQLField})
          }

          buildField(
              root = root,
              path = selfPath,
              info = childInfo,
              selections = childSelections,
              condition = childCondition,
              isSynthetic = true
          )
        }

    /**
     * Merge fragment spreads
     *
     * Since they all have the same shape, it's ok
     */
    val fragmentSpreadFields = selections.map { it.selection }.filterIsInstance<GQLFragmentSpread>()
        .groupBy {
          it.name
        }.values.map { fragmentSpreadsWithSameName ->
          val first = fragmentSpreadsWithSameName.first()

          val fragmentDefinition = allFragmentDefinitions[first.name]!!
          val typeCondition = fragmentDefinition.typeCondition.name

          val possibleTypes = schema.possibleTypes(typeCondition)
          val childCondition = BooleanExpression.Or(fragmentSpreadsWithSameName.map { it.directives.toBooleanExpression() }.toSet())
              .simplify()
              .and(BooleanExpression.Element(BPossibleTypes(possibleTypes)))
              .simplify()

          val fragmentModelId = IrModelId(IrModelRoot(IrRootKind.FragmentData, first.name), "." + first.name)

          val childInfo = IrFieldInfo(
              responseName = first.name.decapitalize().escapeKotlinReservedWord(),
              description = "Synthetic field for '${first.name}'",
              deprecationReason = null,
              type = IrModelType(fragmentModelId)
          )

          val p = if (insertFragmentSyntheticField) {
            "$selfPath.$FRAGMENTS_SYNTHETIC_FIELD"
          } else {
            selfPath
          }
          buildField(
              root = root,
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
          id = IrModelId(root, childPath),
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
        id = IrModelId(root, selfPath),
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
    val id: IrModelId,
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