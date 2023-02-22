package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.api.BLabel
import com.apollographql.apollo3.api.BTerm
import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.api.and
import com.apollographql.apollo3.api.not
import com.apollographql.apollo3.api.or
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableDefinition
import com.apollographql.apollo3.ast.GQLVariableValue
import com.apollographql.apollo3.ast.InferredVariable
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.TransformResult
import com.apollographql.apollo3.ast.coerceInSchemaContextOrThrow
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.findNonnull
import com.apollographql.apollo3.ast.findOptInFeature
import com.apollographql.apollo3.ast.inferVariables
import com.apollographql.apollo3.ast.isFieldNonNull
import com.apollographql.apollo3.ast.optionalValue
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.rawType
import com.apollographql.apollo3.ast.responseName
import com.apollographql.apollo3.ast.rootTypeDefinition
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.transform
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED_WITH_INTERFACES
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED

internal class IrOperationsBuilder(
    private val schema: Schema,
    private val operationDefinitions: List<GQLOperationDefinition>,
    private val fragmentDefinitions: List<GQLFragmentDefinition>,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val codegenModels: String,
    private val generateOptionalOperationVariables: Boolean,
    private val fieldsOnDisjointTypesMustMerge: Boolean,
    private val flattenModels: Boolean,
    private val decapitalizeFields: Boolean,
    private val alwaysGenerateTypesMatching: Set<String>,
    private val generateDataBuilders: Boolean,
) : FieldMerger {
  private val usedTypes = mutableSetOf<String>()
  private val usedFields = mutableMapOf<String, MutableSet<String>>()

  private val responseBasedBuilder = ResponseBasedModelGroupBuilder(
      schema,
      allFragmentDefinitions,
      this
  )

  private val builder = when (codegenModels) {
    MODELS_OPERATION_BASED -> OperationBasedModelGroupBuilder(
        schema = schema,
        allFragmentDefinitions = allFragmentDefinitions,
        fieldMerger = this,
    )

    MODELS_OPERATION_BASED_WITH_INTERFACES -> OperationBasedWithInterfacesModelGroupBuilder(
        schema = schema,
        allFragmentDefinitions = allFragmentDefinitions,
        fieldMerger = this,
    )

    MODELS_RESPONSE_BASED -> responseBasedBuilder
    else -> error("codegenModels='$codegenModels' is not supported")
  }

  private fun shouldAlwaysGenerate(name: String) = alwaysGenerateTypesMatching.map { Regex(it) }.any { it.matches(name) }

  fun build(): IrOperations {
    val operations = operationDefinitions.map { it.toIr() }
    val fragments = fragmentDefinitions.map { it.toIr() }

    /**
     * Add schema types that are requested by the user
     */
    schema.typeDefinitions.values.forEach {gqlTypeDefinition ->
      if (shouldAlwaysGenerate(gqlTypeDefinition.name)) {
        usedTypes.add(gqlTypeDefinition.name)
      }

      val fields = when(gqlTypeDefinition) {
        is GQLObjectTypeDefinition -> gqlTypeDefinition.fields
        is GQLInterfaceTypeDefinition -> gqlTypeDefinition.fields
        else -> null
      }

      if (fields != null) {
        fields.forEach {
          if (shouldAlwaysGenerate("${gqlTypeDefinition.name}.${it.name}")) {
            usedFields.putField(gqlTypeDefinition.name, it.name)
          }
        }
      }
    }

    /**
     * Add the types that are needed by data builders
     * Hydrate the output types.
     * No need to add recursively here, this was done while scanning the operations already.
     * visitedOutputTypes contains a map of type -> set of used fields
     */
    val hydratedUsedFields = mutableMapOf<String, MutableSet<String>>()
    if (generateDataBuilders) {
      usedFields.entries.forEach { entry ->
        when (val typeDefinition = schema.typeDefinition(entry.key)) {
          is GQLObjectTypeDefinition -> {
            typeDefinition.implementsInterfaces.forEach {
              /**
               * Make sure data builders generate the map interface
               */
              hydratedUsedFields.putType(it)
            }
            hydratedUsedFields.putAllFields(typeDefinition.name, entry.value)
          }

          is GQLInterfaceTypeDefinition -> {
            /**
             * Add all possible types because the user might want to use any of them
             * GetHeroQuery.Data {
             *   hero = buildHuman {}  // or buildDroid {}
             * }
             */
            schema.typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>()
                .filter {
                  it.implementsInterfaces.contains(typeDefinition.name)
                }
                .forEach {
                  hydratedUsedFields.putAllFields(it.name, entry.value)
                }

            /**
             * And also add the fields on the interface itself for building the "Other" builder.
             * Note that this over-generates a bit in cases where fragments conditions are always true:
             *
             * ```graphql
             * {
             *   cat {
             *     ... on Animal {
             *       species
             *     }
             *   }
             * }
             * ```
             *
             * In the above case, there's no reason a data builder would need to create a fallback animal
             * yet `buildOtherAnimal` will be generated
             */
            hydratedUsedFields.putAllFields(typeDefinition.name, entry.value)
          }

          is GQLUnionTypeDefinition -> {
            /**
             * Same logic as for interface above
             */
            typeDefinition.memberTypes.forEach {
              hydratedUsedFields.putAllFields(it.name, entry.value)
            }
            hydratedUsedFields.putAllFields(typeDefinition.name, entry.value)
          }

          else -> Unit // Nothing to do
        }
      }

      usedTypes += hydratedUsedFields.keys
    }

    /**
     * Add schema types that are referenced indirectly
     */
    val visitedNames = mutableSetOf<String>()
    val namesStack = usedTypes.toMutableList()
    while (namesStack.isNotEmpty()) {
      val name = namesStack.removeFirst()
      if (visitedNames.contains(name)) {
        continue
      }
      visitedNames.add(name)

      when(val typeDefinition = schema.typeDefinition(name)) {
        is GQLInputObjectTypeDefinition -> {
          /**
           * Loop on the input types.
           * Recursively add all their input fields types.
           * Note that input types may contain cycles, so we have to keep track of visited types to
           * avoid looping endlessly (in addition to not computing them multiple times)
           */
          typeDefinition.inputFields.forEach {
            when (val fieldType = schema.typeDefinition(it.type.rawType().name)) {
              is GQLScalarTypeDefinition -> namesStack.add(fieldType.name)
              is GQLEnumTypeDefinition -> namesStack.add(fieldType.name)
              is GQLInputObjectTypeDefinition -> namesStack.add(fieldType.name)
              else -> error("output type '${fieldType.name}' used in input position")
            }
          }
        }

        is GQLUnionTypeDefinition -> {
          /**
           * Unions reference their members, generate them:
           *
           * public class SearchResult {
           *   public companion object {
           *     public val type: UnionType = UnionType("SearchResult", Human.type, Droid.type, Starship.type)
           *   }
           * }
           */
          typeDefinition.memberTypes.forEach {
            namesStack.add(it.name)
          }
        }
        is GQLInterfaceTypeDefinition -> {
          /**
           * Interface classes reference their super interfaces, generate them:
           *
           * public class Character {
           *   public companion object {
           *     public val type: InterfaceType = InterfaceType.Builder(name = "Character").build()
           *   }
           * }
           */
          typeDefinition.implementsInterfaces.forEach {
            namesStack.add(it)
          }
        }
        is GQLObjectTypeDefinition -> {
          /**
           * Object classes reference their super interfaces, generate them:
           *
           * public class Droid {
           *   public companion object {
           *     public val type: ObjectType =
           *         ObjectType.Builder(name = "Droid").interfaces(listOf(Character.type)).build()
           *   }
           * }
           */
          typeDefinition.implementsInterfaces.forEach {
            namesStack.add(it)
          }

          if (generateDataBuilders) {
            /**
             * DataBuilder maps reference all their super types, including unions
             *
             * internal class DroidMap(
             *   __fields: Map<String, Any?>,
             * ) : CharacterMap, SearchResultMap, Map<String, Any?> by __fields
             */
            schema.typeDefinitions.values.filterIsInstance<GQLUnionTypeDefinition>().filter {
              it.memberTypes.any {
                it.name == typeDefinition.name
              }
            }.forEach {
              namesStack.add(it.name)
            }
          }
        }
      }
    }


    return DefaultIrOperations(
        operations = operations,
        fragments = fragments,
        usedTypes = visitedNames,
        usedFields = hydratedUsedFields,
        codegenModels = codegenModels,
        flattenModels = flattenModels,
        decapitalizeFields = decapitalizeFields,
        fragmentDefinitions = fragmentDefinitions,
        generateDataBuilders = generateDataBuilders,
    )
  }

  /**
   * Strip any custom Apollo directive and format
   */
  private fun GQLNode.formatToString(): String {
    return transform {
      if (it is GQLDirective && schema.shouldStrip(it.name)) {
        TransformResult.Delete
      } else {
        TransformResult.Continue
      }
    }!!.toUtf8()
  }

  private fun GQLOperationDefinition.toIr(): IrOperation {
    val typeDefinition = this.rootTypeDefinition(schema)
        ?: throw IllegalStateException("ApolloGraphql: cannot find root type for '$operationType'")

    check(name != null) {
      "Apollo doesn't support anonymous operation."
    }

    val usedFragments = usedFragments(
        schema = schema,
        allFragmentDefinitions,
        selections = selectionSet.selections,
        rawTypename = typeDefinition.name,
    )

    val sourceWithFragments = (formatToString() + "\n" + usedFragments.joinToString(
        separator = "\n"
    ) { fragmentName ->
      allFragmentDefinitions[fragmentName]!!.formatToString()
    }).trimEnd('\n')

    val (dataProperty, dataModelGroup) = builder.buildOperationData(
        selections = selectionSet.selections,
        rawTypeName = typeDefinition.name,
        operationName = name!!
    )

    val responseBasedModelGroup = when (codegenModels) {
      MODELS_RESPONSE_BASED -> dataModelGroup
      else -> null
    }
    // Add the root type to use from the selections
    usedTypes.add(typeDefinition.name)
    return IrOperation(
        name = name!!,
        description = description,
        operationType = operationType.toIrOperationType(schema.rootTypeNameFor(operationType)),
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        selectionSets = SelectionSetsBuilder(schema, allFragmentDefinitions).build(selectionSet.selections, typeDefinition.name, variableDefinitions),
        sourceWithFragments = sourceWithFragments,
        filePath = sourceLocation.filePath!!,
        dataProperty = dataProperty,
        dataModelGroup = dataModelGroup,
        responseBasedDataModelGroup = responseBasedModelGroup
    )
  }

  private fun String.toIrOperationType(typeName: String) = when (this) {
    "query" -> IrOperationType.Query(typeName)
    "mutation" -> IrOperationType.Mutation(typeName)
    "subscription" -> IrOperationType.Subscription(typeName)
    else -> error("unknown operation $this")
  }

  private fun GQLFragmentDefinition.toIr(): IrFragmentDefinition {
    val typeDefinition = schema.typeDefinition(typeCondition.name)

    val inferredVariables = inferVariables(schema, allFragmentDefinitions, fieldsOnDisjointTypesMustMerge)

    val interfaceModelGroup = builder.buildFragmentInterface(
        fragmentName = name
    )

    val (dataProperty, dataModelGroup) = builder.buildFragmentData(
        fragmentName = name
    )

    // Add the root type to use from the fragment selections
    usedTypes.add(typeCondition.name)
    return IrFragmentDefinition(
        name = name,
        description = description,
        filePath = sourceLocation.filePath!!,
        typeCondition = typeDefinition.name,
        variables = inferredVariables.map { it.toIr() },
        selectionSets = SelectionSetsBuilder(schema, allFragmentDefinitions).build(selectionSet.selections, typeCondition.name, emptyList()),
        interfaceModelGroup = interfaceModelGroup,
        dataProperty = dataProperty,
        dataModelGroup = dataModelGroup,
        source = formatToString()
    )
  }

  private fun InferredVariable.toIr(): IrVariable {
    var irType = type.toIr()
    if (type !is GQLNonNullType) {
      // If the type is nullable, the variable might be omitted, make it optional
      irType = irType.makeOptional()
    }
    return IrVariable(
        name = name,
        defaultValue = null,
        type = irType,
    )
  }

  private fun GQLVariableDefinition.toIr(): IrVariable {
    val coercedDefaultValue = defaultValue?.coerceInSchemaContextOrThrow(type, schema)

    var irType = type.toIr()
    when {
      irType is IrNonNullType && coercedDefaultValue == null -> {
        // The variable is non-nullable and has no defaultValue => it must always be sent
        // Leave irType as-is
      }

      coercedDefaultValue != null -> {
        // the variable has a defaultValue meaning that there is a use case for not providing it
        irType = irType.makeOptional()
      }

      else -> {
        // The variable is nullable. By the GraphQL spec, it means it's also optional
        // In practice though, we often want it non-optional, because if the user added tha variable in
        // the first place, there is a high change they're going to use it.
        //
        // One counter example is bidirectional pagination where 'before' or 'after' could be
        // left Absent
        //
        // We default to add the [Optional] wrapper, but this can be overridden by the user globally or individually
        // with the @optional directive.

        val makeOptional = directives.optionalValue(schema) ?: generateOptionalOperationVariables
        if (makeOptional) {
          irType = irType.makeOptional()
        }
      }
    }

    return IrVariable(
        name = name,
        defaultValue = coercedDefaultValue?.toIrValue(),
        type = irType,
    )
  }

  /**
   * Maps to [IrType]
   */
  private fun GQLType.toIr(): IrType {
    usedTypes.add(rawType().name)
    return toIr(schema)
  }

  /**
   * An intermediate class used during collection
   */
  private class CollectedField(
      /**
       * All fields with the same response name should have the same info here
       */
      val name: String,
      val alias: String?,

      val description: String?,
      val type: GQLType,
      val deprecationReason: String?,
      val optInFeature: String?,
      val forceNonNull: Boolean,
      val forceOptional: Boolean,

      /**
       * Merged field will merge their conditions and selectionSets
       */
      val condition: BooleanExpression<BVariable>,
      val selections: List<GQLSelection>,
      val parentType: String,
  ) {
    val responseName = alias ?: name
  }

  private fun MutableMap<String, MutableSet<String>>.putField(typeName: String, fieldName: String) {
    compute(typeName) { _, v ->
      if (v == null) {
        mutableSetOf(fieldName)
      } else {
        v.add(fieldName)
        v
      }
    }
  }

  private fun MutableMap<String, MutableSet<String>>.putAllFields(typeName: String, fields: MutableSet<String>) {
    compute(typeName) { _, v ->
      (v ?: mutableSetOf()).apply {
        this.addAll(fields)
      }
    }
  }

  private fun MutableMap<String, MutableSet<String>>.putType(typeName: String) {
    compute(typeName) { _, v ->
      v ?: mutableSetOf()
    }
  }

  override fun merge(fields: List<FieldWithParent>): List<MergedField> {
    return fields.map { fieldWithParent ->
      val gqlField = fieldWithParent.gqlField
      val parentTypeDefinition = schema.typeDefinition(fieldWithParent.parentType)
      val fieldDefinition = gqlField.definitionFromScope(schema, parentTypeDefinition)

      check(fieldDefinition != null) {
        "cannot find field definition for field '${gqlField.responseName()}' of type '${parentTypeDefinition.name}'"
      }
      val forceNonNull = gqlField.directives.findNonnull(schema) || parentTypeDefinition.isFieldNonNull(gqlField.name, schema)

      /**
       * It's ok to always pass the empty list for variableDefinitions because the parsers do not care about @include and @skip
       * All they care about is that the model types match what is sent by the server
       */
      val variableDefinitions: List<GQLVariableDefinition> = emptyList()
      val condition = gqlField.directives.toIncludeBooleanExpression(variableDefinitions)

      CollectedField(
          name = gqlField.name,
          alias = gqlField.alias,
          condition = condition,
          selections = gqlField.selectionSet?.selections ?: emptyList(),
          type = fieldDefinition.type,
          description = fieldDefinition.description,
          deprecationReason = fieldDefinition.directives.findDeprecationReason(),
          optInFeature = fieldDefinition.directives.findOptInFeature(schema),
          forceNonNull = forceNonNull,
          forceOptional = gqlField.directives.optionalValue(schema) == true,
          parentType = fieldWithParent.parentType,
      )
    }.groupBy {
      it.responseName
    }.values.map { fieldsWithSameResponseName ->

      /**
       * Sanity checks, might be removed as this should be done during validation
       */
      check(fieldsWithSameResponseName.map { it.alias }.distinct().size == 1)
      check(fieldsWithSameResponseName.map { it.name }.distinct().size == 1)
      // GQLTypes might differ because of their source location. Use pretty()
      // to canonicalize them
      check(fieldsWithSameResponseName.map { it.type }.distinctBy { it.pretty() }.size == 1)

      val first = fieldsWithSameResponseName.first()
      val childSelections = fieldsWithSameResponseName.flatMap { it.selections }

      val forceNonNull = fieldsWithSameResponseName.any {
        it.forceNonNull
      }
      val forceOptional = fieldsWithSameResponseName.any {
        it.forceOptional
      }

      usedFields.putField(first.parentType, first.name)
      /**
       * We track field usages, but we also need to track the type itself because it might be that there is only fragments
       * node {
       *   # no field here but Node is still used
       *   ... on Product {
       *     price
       *   }
       * }
       */
      usedFields.putType(first.type.rawType().name)

      var irType = first.type.toIr()
      if (forceNonNull) {
        irType = irType.makeNonNull()
      } else if (forceOptional) {
        irType = irType.makeNullable()
      }

      /**
       * Depending on the parent object/interface in which the field is queried, the field definition might have different descriptions/deprecationReasons
       */
      val description = fieldsWithSameResponseName.associateBy { it.description }.values.let { descriptionCandidates ->
        if (descriptionCandidates.size == 1) {
          descriptionCandidates.single().description
        } else {
          val parents = descriptionCandidates.map { it.parentType }
          "Merged field with multiple descriptions. See parentTypes: '${parents.joinToString(", ")}' for more information"
        }
      }

      val deprecationReason = fieldsWithSameResponseName.associateBy { it.deprecationReason }.values.let { deprecationCandidates ->
        if (deprecationCandidates.size == 1) {
          deprecationCandidates.single().deprecationReason
        } else {
          val parents = deprecationCandidates.filter { it.deprecationReason != null }.map { it.parentType }
          "Deprecated in: '${parents.joinToString(", ")}'"
        }
      }
      val optInFeature = fieldsWithSameResponseName.associateBy { it.optInFeature }.values.let { experimentalCandidates ->
        if (experimentalCandidates.size == 1) {
          experimentalCandidates.single().optInFeature
        } else {
          val parents = experimentalCandidates.filter { it.optInFeature != null }.map { it.parentType }
          "Experimental in: '${parents.joinToString(", ")}'"
        }
      }

      val info = IrFieldInfo(
          responseName = first.alias ?: first.name,
          description = description,
          deprecationReason = deprecationReason,
          optInFeature = optInFeature,
          type = irType,
          gqlType = first.type,
      )

      MergedField(
          info = info,
          condition = BooleanExpression.Or(fieldsWithSameResponseName.map { it.condition }.toSet()).simplify(),
          selections = childSelections,
          rawTypeName = first.type.rawType().name,
      )
    }
  }
}

internal fun GQLValue.toIrValue(): IrValue {
  return when (this) {
    is GQLIntValue -> IrIntValue(value = value)
    is GQLStringValue -> IrStringValue(value = value)
    is GQLFloatValue -> IrFloatValue(value = value)
    is GQLBooleanValue -> IrBooleanValue(value = value)
    is GQLEnumValue -> IrEnumValue(value = value)
    is GQLNullValue -> IrNullValue
    is GQLVariableValue -> IrVariableValue(name = name)
    is GQLListValue -> IrListValue(values = values.map { it.toIrValue() })
    is GQLObjectValue -> IrObjectValue(
        fields = fields.map {
          IrObjectValue.Field(name = it.name, value = it.value.toIrValue())
        }
    )
  }
}

/**
 * This is guaranteed to return one of:
 * - True
 * - False
 * - (!)Variable
 * - (!)Variable & (!)Variable
 */
internal fun List<GQLDirective>.toIncludeBooleanExpression(variableDefinitions: List<GQLVariableDefinition>): BooleanExpression<BVariable> {
  val conditions = mapNotNull {
    it.toIncludeBooleanExpression(variableDefinitions)
  }
  return if (conditions.isEmpty()) {
    BooleanExpression.True
  } else {
    check(conditions.toSet().size == conditions.size) {
      "Apollo: duplicate @skip/@include directives are not allowed"
    }
    // Having both @skip and @include is allowed
    // In that case, it's equivalent to an "And"
    // See https://spec.graphql.org/draft/#sec--include
    BooleanExpression.And(conditions.toSet()).simplify()
  }
}

internal fun GQLDirective.toIncludeBooleanExpression(variableDefinitions: List<GQLVariableDefinition>): BooleanExpression<BVariable>? {
  if (setOf("skip", "include").contains(name).not()) {
    // not a condition directive
    return null
  }
  if (arguments?.arguments?.size != 1) {
    throw IllegalStateException("Apollo: wrong number of arguments for '$name' directive: ${arguments?.arguments?.size}")
  }

  val argument = arguments!!.arguments.first()

  return when (val value = argument.value) {
    is GQLBooleanValue -> {
      if (value.value) BooleanExpression.True else BooleanExpression.False
    }

    is GQLVariableValue -> {
      val defaultValue = (variableDefinitions.firstOrNull { it.name == value.name }?.defaultValue as? GQLBooleanValue?)?.value
      BooleanExpression.Element(BVariable(name = value.name, defaultValue = defaultValue))
    }
    else -> throw IllegalStateException("Apollo: cannot pass ${value.toUtf8()} to '$name' directive")
  }.let {
    if (name == "skip") not(it) else it
  }
}

/**
 * A combination of the result of [toIncludeBooleanExpression] and either `True` or a [BLabel].
 */
internal fun List<GQLDirective>.toBooleanExpression(): BooleanExpression<BTerm> {
  val deferBooleanConditions = mapNotNull {
    it.toDeferBooleanExpression()
  }
  val deferBooleanExpression = if (deferBooleanConditions.isEmpty()) {
    BooleanExpression.True
  } else {
    check(deferBooleanConditions.size == 1) {
      "Apollo: duplicate @defer directives are not allowed"
    }
    deferBooleanConditions.first()
  }
  return toIncludeBooleanExpression(emptyList()).and(deferBooleanExpression).simplify()
}

internal fun GQLDirective.toDeferBooleanExpression(): BooleanExpression<BTerm>? {
  if (name != "defer") return null
  val ifArgumentValue = arguments?.arguments?.firstOrNull { it.name == "if" }?.value ?: GQLBooleanValue(value = true)

  val labelArgumentValue = arguments?.arguments?.firstOrNull { it.name == "label" }?.value
  if (labelArgumentValue != null && labelArgumentValue !is GQLStringValue) throw IllegalStateException("Apollo: cannot pass ${labelArgumentValue.toUtf8()} to 'label' argument of 'defer' directive")
  val label = (labelArgumentValue as GQLStringValue?)?.value
  return when (ifArgumentValue) {
    is GQLBooleanValue -> {
      if (!ifArgumentValue.value) {
        // @defer(if: false) means we should parse
        BooleanExpression.True
      } else {
        BooleanExpression.Element(BLabel(label))
      }
    }

    is GQLVariableValue -> {
      // @defer(label: $lbl1, if: $var1) can be translated to BLabel("lbl1") || !BVariable("var1")
      BooleanExpression.Element(BLabel(label)).or(not(BooleanExpression.Element(BVariable(ifArgumentValue.name))))
    }

    else -> throw IllegalStateException("Apollo: cannot pass ${ifArgumentValue.toUtf8()} to 'if' argument of 'defer' directive")
  }
}

internal fun IrFieldInfo.maybeNullable(makeNullable: Boolean): IrFieldInfo {
  if (!makeNullable) {
    return this
  }

  return copy(
      type = type.makeNullable()
  )
}
