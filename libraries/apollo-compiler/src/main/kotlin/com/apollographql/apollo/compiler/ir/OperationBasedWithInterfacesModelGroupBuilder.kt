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
import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.modelName
import com.apollographql.apollo.compiler.decapitalizeFirstLetter
import com.apollographql.apollo.compiler.internal.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.lowerCamelCaseIgnoringNonLetters

/**
 * Very similar to [OperationBasedModelGroupBuilder] except:
 * - it doesn't support compat
 * - it doesn't support `@include` and `@defer`
 * - it generates interfaces for polymorphic selection sets
 *
 * For that last point, it starts just like [OperationBasedModelGroupBuilder] and once it has a tree of nodes,
 * it converts nodes that contains fragments into a modelGroup that has multiple models
 */
internal class OperationBasedWithInterfacesModelGroupBuilder(
    private val schema: Schema,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldMerger: FieldMerger,
) : ModelGroupBuilder {

  override fun buildOperationData(
      selections: List<GQLSelection>,
      rawTypeName: String,
      operationName: String,
      defaultCatchTo: CatchTo?,
  ): Pair<IrProperty, IrModelGroup> {
    check(defaultCatchTo == null) {
      "Apollo: operationBasedWithInterfaces codegen does not support @catch"
    }
    val info = IrFieldInfo(
        responseName = "data",
        description = null,
        type = IrModelType(MODEL_UNKNOWN),
        deprecationReason = null,
        optInFeature = null,
        gqlType = GQLNonNullType(type = GQLNamedType(name = rawTypeName))
    )

    val field = buildNode(
        path = "${MODEL_OPERATION_DATA}.$operationName",
        info = info,
        parentTypes = listOf(rawTypeName),
        selections = selections,
        condition = BooleanExpression.True
    )

    return field.toProperty() to field.toModelGroup()!!
  }

  override fun buildFragmentInterface(fragmentName: String): IrModelGroup? {
    return null
  }

  override fun buildFragmentData(fragmentName: String, defaultCatchTo: CatchTo?): Pair<IrProperty, IrModelGroup> {
    check(defaultCatchTo == null) {
      "Apollo: operationBasedWithInterfaces codegen does not support @catch"
    }

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

    val field = buildNode(
        path = "${MODEL_FRAGMENT_DATA}.$fragmentName",
        info = info,
        selections = mergedSelections,
        condition = BooleanExpression.True,
        parentTypes = listOf(fragmentDefinition.typeCondition.name),
    )

    return field.toProperty() to field.toModelGroup()!!
  }

  /**
   * Build a field, inline fragment or fragment definition.
   *
   * @param path the path up to but not including this selection
   * @param parentTypes the list of the different typeConditions going from the field type through all inline fragments
   * - for a field, this is the rawTypeName of the field
   * - for an inline fragment, this is the typeCondition of the fragment and all previous inline fragment:
   * ```
   * {
   *   node {
   *     ... on Character {
   *       # building this inline fragment, parentTypes = listOf("Node", "Character", "Droid")
   *       ... on Droid {
   *       }
   *     }
   *   }
   * }
   * ```
   * - for a fragment definition this is the typeCondition of this fragment definition
   *
   * @param info information about this selection
   * @param selections the sub-selections of this selection. They are all on the same parentType
   * @param condition the condition for this field. A mix of include directives, @defer labels and type conditions
   */
  private fun buildNode(
      path: String,
      parentTypes: List<String>,
      info: IrFieldInfo,
      selections: List<GQLSelection>,
      condition: BooleanExpression<BTerm>,
  ): OperationField2 {
    if (selections.isEmpty()) {
      return OperationField2(
          info = info,
          condition = condition,
          fieldSet = null,
          parentTypes = parentTypes
      )
    }

    val selfPath = path + "." + info.responseName
    val parentType = parentTypes.last()

    /**
     * Merge fragments with the same type condition
     *
     * We do not support `@include`/`@skip` directives here
     */
    val inlineFragmentsFields = selections.filterIsInstance<GQLInlineFragment>()
        .groupBy { it.typeCondition?.name ?: parentType }
        .entries.map { entry ->
          val name = "on${entry.key.capitalizeFirstLetter()}"
          val typeCondition = entry.key

          val childCondition: BooleanExpression<BTerm> = if (parentTypes.any { schema.isTypeASubTypeOf(it, typeCondition) }) {
            /**
             * If any of the parent types is a subtype of the type condition (e.g. Cat is a subtype of Animal) then we can skip checking the typename
             */
            BooleanExpression.True
          } else {
            val possibleTypes = schema.possibleTypes(typeCondition)
            BooleanExpression.Element(BPossibleTypes(possibleTypes))
          }

          var type: IrType = IrModelType(MODEL_UNKNOWN, nullable = true)
          if (childCondition == BooleanExpression.True) {
            type = type.nullable(false)
          }

          val childInfo = IrFieldInfo(
              responseName = name,
              description = "Synthetic field for inline fragment on $typeCondition",
              deprecationReason = null,
              optInFeature = null,
              type = type,
              gqlType = null,
          )

          val childSelections = entry.value.flatMap {
            it.selections
          }

          buildNode(
              path = selfPath,
              info = childInfo,
              selections = childSelections,
              condition = childCondition,
              parentTypes = parentTypes + typeCondition,
          )
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

          val childCondition: BooleanExpression<BTerm> = if (parentTypes.any { schema.isTypeASubTypeOf(it, typeCondition) }) {
            /**
             * If any of the parent types is a subtype (e.g. Cat is a subtype of Animal) then we can skip checking the typename
             */
            BooleanExpression.True
          } else {
            val possibleTypes = schema.possibleTypes(typeCondition)
            BooleanExpression.Element(BPossibleTypes(possibleTypes))
          }

          /**
           * Beware the double first.name because:
           * - first one is the fragment name
           * - second one is the name of the field (there could be multiple root classes/interfaces for fragments)
           */
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

          buildNode(
              path = selfPath,
              info = childInfo,
              selections = emptyList(), // Don't create a model for fragments spreads
              condition = childCondition,
              parentTypes = parentTypes + typeCondition
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

    val fields = fieldMerger.merge(fieldsWithParent, null).map { mergedField ->
      val childInfo = mergedField.info.maybeNullable(mergedField.condition != BooleanExpression.True)

      buildNode(
          path = selfPath,
          info = childInfo,
          selections = mergedField.selections,
          condition = BooleanExpression.True,
          parentTypes = listOf(mergedField.rawTypeName)
      )
    }

    val fieldSet = OperationFieldSet2(
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

    return OperationField2(
        info = patchedInfo,
        condition = condition,
        parentTypes = parentTypes,
        fieldSet = fieldSet,
    )
  }


  private class OperationField2(
      val info: IrFieldInfo,
      val condition: BooleanExpression<BTerm>,
      val parentTypes: List<String>,
      val fieldSet: OperationFieldSet2?,
  ) {
    val isSynthetic: Boolean
      get() = info.gqlType == null

  }

  private data class OperationFieldSet2(
      val id: String,
      val modelName: String,
      val fields: List<OperationField2>,
  )

  private fun OperationField2.toModelGroup(): IrModelGroup? {
    if (fieldSet == null) {
      // fast path: leaf field
      return null
    }

    val syntheticFields = fieldSet.fields.filter { it.isSynthetic }
    val selfTypeCondition = parentTypes.last()
    // TODO: cache this computation
    val incomingTypes = parentTypes.map { schema.possibleTypes(it) }.intersection().toList()
    val typeSets = syntheticFields.map {
      setOf(selfTypeCondition, it.parentTypes.last())
    }
    var buckets = buckets(schema, incomingTypes, typeSets)
    if (buckets.none { it.typeSet.size == 1 }) {
      /**
       * Add the fallback type if required
       */
      buckets = buckets + Bucket(setOf(selfTypeCondition), incomingTypes.toSet())
    }

    val models = if (buckets.size == 1) {
      listOf(fieldSet.toClass())
    } else {
      listOf(fieldSet.toInterface()) + buckets.map { fieldSet.toSubClass(info, selfTypeCondition, it) }
    }

    return IrModelGroup(
        models = models,
        baseModelId = fieldSet.id
    )
  }


  private fun OperationFieldSet2.toClass(): IrModel {
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

  private fun OperationFieldSet2.toInterface(): IrModel {
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

  private fun OperationFieldSet2.toSubClass(fieldInfo: IrFieldInfo, rawTypeName: String, bucket: Bucket): IrModel {
    val isOther = bucket.typeSet.size == 1
    val modelName = modelName(fieldInfo, bucket.typeSet, rawTypeName, isOther)
    return IrModel(
        modelName = modelName,
        id = id.substringBeforeLast(".") + modelName,
        properties = fields.map { it.toOverriddenProperty(bucket.possibleTypes) },
        accessors = emptyList(),
        implements = listOf(id),
        isFallback = isOther,
        isInterface = false,
        modelGroups = emptyList(),
        possibleTypes = bucket.possibleTypes.toList(),
        typeSet = emptySet(),
    )
  }

  private fun OperationField2.toProperty(): IrProperty {
    return IrProperty(
        info = info,
        override = false,
        condition = condition,
        requiresBuffering = fieldSet?.fields?.any { it.isSynthetic } ?: false,
    )
  }

  private fun OperationField2.toOverriddenProperty(possibleTypes: PossibleTypes): IrProperty {
    val info: IrFieldInfo
    val condition: BooleanExpression<BTerm>

    if (isSynthetic) {
      condition = this.condition.simplify(possibleTypes)
      info = if (condition is BooleanExpression.True && this.info.type.nullable) {
        this.info.copy(type = this.info.type.nullable(false))
      } else {
        this.info
      }
    } else {
      info = this.info
      condition = this.condition
    }
    return IrProperty(
        info = info,
        override = true,
        condition = condition,
        requiresBuffering = fieldSet?.fields?.any { it.isSynthetic } ?: false,
    )
  }


  private fun BooleanExpression<BTerm>.simplify(possibleTypes: PossibleTypes): BooleanExpression<BTerm> {
    if (this !is BooleanExpression.Element) {
      return this
    }

    val value = this.value
    if (value !is BPossibleTypes) {
      return this
    }

    if(value.possibleTypes.containsAll(possibleTypes)) {
      return BooleanExpression.True
    }

    return this
  }
}
