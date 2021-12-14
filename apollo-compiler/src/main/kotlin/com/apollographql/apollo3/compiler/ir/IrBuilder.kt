package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.api.not
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLEnumValueDefinition
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInputValueDefinition
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamedType
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
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.TransformResult
import com.apollographql.apollo3.ast.VariableReference
import com.apollographql.apollo3.ast.coerceInExecutableContextOrThrow
import com.apollographql.apollo3.ast.coerceInSchemaContextOrThrow
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.findNonnull
import com.apollographql.apollo3.ast.inferVariables
import com.apollographql.apollo3.ast.isApollo
import com.apollographql.apollo3.ast.isFieldNonNull
import com.apollographql.apollo3.ast.leafType
import com.apollographql.apollo3.ast.optionalValue
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.responseName
import com.apollographql.apollo3.ast.rootTypeDefinition
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.transform
import com.apollographql.apollo3.compiler.MODELS_COMPAT
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED

@OptIn(ApolloExperimental::class)
internal class IrBuilder(
    private val schema: Schema,
    private val operationDefinitions: List<GQLOperationDefinition>,
    private val alwaysGenerateResponseBasedDataModelGroup: Boolean,
    private val fragmentDefinitions: List<GQLFragmentDefinition>,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val alwaysGenerateTypesMatching: Set<String>,
    private val customScalarsMapping: Map<String, String>,
    private val codegenModels: String,
    private val generateOptionalOperationVariables: Boolean,
) : FieldMerger {
  private val usedTypes = mutableListOf<String>()

  private val responseBasedBuilder = ResponseBasedModelGroupBuilder(
        schema,
        allFragmentDefinitions,
        this
    )

  private val builder = when(codegenModels) {
    @Suppress("DEPRECATION")
    MODELS_COMPAT -> OperationBasedModelGroupBuilder(
        schema = schema,
        allFragmentDefinitions = allFragmentDefinitions,
        fieldMerger = this,
        compat = true,
    )
    MODELS_OPERATION_BASED -> OperationBasedModelGroupBuilder(
        schema = schema,
        allFragmentDefinitions = allFragmentDefinitions,
        fieldMerger = this,
        compat = false,
    )
    MODELS_RESPONSE_BASED -> responseBasedBuilder
    else -> error("codegenModels='$codegenModels' is not supported")
  }
  private fun shouldAlwaysGenerate(name: String) = alwaysGenerateTypesMatching.map { Regex(it) }.any { it.matches(name) }

  fun build(): Ir {
    val operations = operationDefinitions.map { it.toIr() }
    val fragments = fragmentDefinitions.map { it.toIr() }

    val visitedTypes = mutableSetOf<String>()
    val enums = mutableListOf<IrEnum>()
    val inputObjects = mutableListOf<IrInputObject>()
    val objects = mutableListOf<IrObject>()
    val interfaces = mutableListOf<IrInterface>()
    val unions = mutableListOf<IrUnion>()
    val customScalars = mutableListOf<IrCustomScalar>()

    // inject extra types
    usedTypes.addAll(schema.typeDefinitions.keys.filter { shouldAlwaysGenerate(it) })
    // inject custom scalars specified in the Gradle configuration
    usedTypes.addAll(customScalarsMapping.keys)

    // Generate the root types
    operationDefinitions.forEach {
      when (it.operationType) {
        "query" -> usedTypes.add(schema.queryTypeDefinition.name)
        "mutation" -> usedTypes.add(schema.mutationTypeDefinition?.name ?: error("No mutation type"))
        "subscription" -> usedTypes.add(schema.subscriptionTypeDefinition?.name ?: error("No subscription type"))
      }
    }
    // Generate the fragment types
    fragmentDefinitions.forEach {
      usedTypes.add(it.typeCondition.name)
    }

    // Input objects and Interfaces contain (possible reentrant) references so we need to loop here
    while (usedTypes.isNotEmpty()) {
      val name = usedTypes.removeAt(0)
      if (visitedTypes.contains(name)) {
        continue
      }
      visitedTypes.add(name)
      val typeDefinition = schema.typeDefinition(name)
      if (typeDefinition.isBuiltIn()) {
        // We don't generate builtin types
        continue
      }

      when (typeDefinition) {
        is GQLEnumTypeDefinition -> enums.add(typeDefinition.toIr())
        is GQLObjectTypeDefinition -> objects.add(typeDefinition.toIr())
        is GQLUnionTypeDefinition -> unions.add(typeDefinition.toIr())
        is GQLInterfaceTypeDefinition -> interfaces.add(typeDefinition.toIr())
        is GQLScalarTypeDefinition -> customScalars.add(typeDefinition.toIr())
        is GQLInputObjectTypeDefinition -> inputObjects.add(typeDefinition.toIr())
      }
    }

    return Ir(
        operations = operations,
        fragments = fragments,
        inputObjects = inputObjects,
        enums = enums,
        customScalars = customScalars,
        objects = objects,
        interfaces = interfaces,
        unions = unions,
        allFragmentDefinitions = allFragmentDefinitions,
        schema = schema
    )
  }

  private fun GQLObjectTypeDefinition.toIr(): IrObject {
    // Needed to build the compiled type
    usedTypes.addAll(implementsInterfaces)

    return IrObject(
        name = name,
        implements = implementsInterfaces,
        keyFields = schema.keyFields(name),
        description = description,
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLInterfaceTypeDefinition.toIr(): IrInterface {
    // Needed to build the compiled type
    usedTypes.addAll(implementsInterfaces)

    return IrInterface(
        name = name,
        implements = implementsInterfaces,
        keyFields = schema.keyFields(name),
        description = description,
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLUnionTypeDefinition.toIr(): IrUnion {
    // Needed to build the compiled type
    usedTypes.addAll(memberTypes.map { it.name })

    return IrUnion(
        name = name,
        members = memberTypes.map { it.name },
        description = description,
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLScalarTypeDefinition.toIr(): IrCustomScalar {
    return IrCustomScalar(
        name = name,
        kotlinName = customScalarsMapping[name],
        description = description,
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLInputObjectTypeDefinition.toIr(): IrInputObject {
    return IrInputObject(
        name = name,
        description = description,
        deprecationReason = directives.findDeprecationReason(),
        fields = inputFields.map { it.toIrInputField() }
    )
  }

  /**
   * This is not named `toIr` as [GQLInputValueDefinition] also maps to variables and arguments
   */
  private fun GQLInputValueDefinition.toIrInputField(): IrInputField {
    val coercedDefaultValue = defaultValue?.coerceInExecutableContextOrThrow(type, schema)

    var irType = type.toIr()
    if (type !is GQLNonNullType || coercedDefaultValue != null) {
      /**
       * Contrary to [IrVariable], we default to making input fields optional as they are out of control of the user and
       * we don't want to force users to fill all values to define an input object
       */
      irType = irType.makeOptional()
    }
    return IrInputField(
        name = name,
        description = description,
        deprecationReason = directives.findDeprecationReason(),
        type = irType,
        defaultValue = coercedDefaultValue?.toIrValue(),
    )
  }

  private fun GQLEnumTypeDefinition.toIr(): IrEnum {
    return IrEnum(
        name = name,
        description = description,
        values = enumValues.map { it.toIr() }
    )
  }

  private fun GQLEnumValueDefinition.toIr(): IrEnum.Value {
    return IrEnum.Value(
        name = name,
        description = description,
        deprecationReason = directives.findDeprecationReason()
    )
  }

  /**
   * Strip any custom Apollo directive and format
   */
  private fun GQLNode.formatToString(): String {
    return transform {
      if (it is GQLDirective && it.isApollo()) {
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

    val dataModelGroup = builder.buildOperationData(
        selections = selectionSet.selections,
        rawTypeName = typeDefinition.name,
        operationName = name!!
    )

    val responseBasedModelGroup = when {
      codegenModels == MODELS_RESPONSE_BASED -> dataModelGroup
      alwaysGenerateResponseBasedDataModelGroup -> responseBasedBuilder.buildOperationData(
          selections = selectionSet.selections,
          rawTypeName = typeDefinition.name,
          operationName = name!!
      )
      else -> null
    }

    return IrOperation(
        name = name!!,
        description = description,
        operationType = operationType.toIrOperationType(),
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        selections = selectionSet.selections,
        sourceWithFragments = sourceWithFragments,
        filePath = sourceLocation.filePath!!,
        dataModelGroup = dataModelGroup,
        responseBasedDataModelGroup = responseBasedModelGroup
    )
  }

  private fun String.toIrOperationType() = when (this) {
    "query" -> IrOperationType.Query
    "mutation" -> IrOperationType.Mutation
    "subscription" -> IrOperationType.Subscription
    else -> error("unknown operation $this")
  }

  private fun GQLFragmentDefinition.toIr(): IrNamedFragment {
    val typeDefinition = schema.typeDefinition(typeCondition.name)

    val variableDefinitions = inferVariables(schema, allFragmentDefinitions)

    val interfaceModelGroup = builder.buildFragmentInterface(
        fragmentName = name
    )

    val dataModelGroup = builder.buildFragmentData(
        fragmentName = name
    )

    return IrNamedFragment(
        name = name,
        description = description,
        filePath = sourceLocation.filePath,
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        selections = selectionSet.selections,
        interfaceModelGroup = interfaceModelGroup,
        dataModelGroup = dataModelGroup,
    )
  }

  private fun VariableReference.toIr(): IrVariable {
    var type = expectedType.toIr()
    // This is an inferred variable from a fragment
    if (expectedType !is GQLNonNullType) {
      type = type.makeOptional()
    }
    return IrVariable(
        name = this.variable.name,
        defaultValue = null,
        type = type,
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
      irType !is IrNonNullType -> {
        // The variable is nullable. By the GraphQL spec, it means it's also optional
        // In practice though, we often want it non optional, because if the user added tha variable in
        // the first place, there is a high change they're going to use it.
        //
        // One counter example is bi-directional pagination where 'before' or 'after' could be
        // left Absent
        //
        // We default to add the [Optional] wrapper, but this can be overridden by the user globally or individually
        // with the @optional directive.

        val makeOptional = directives.optionalValue() ?: generateOptionalOperationVariables
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
   * Maps to [IrType] and also keep tracks of what types are actually used so we only generate those
   */
  private fun GQLType.toIr(): IrType {
    return when (this) {
      is GQLNonNullType -> IrNonNullType(ofType = type.toIr())
      is GQLListType -> IrListType(ofType = type.toIr())
      is GQLNamedType -> {
        usedTypes.add(name)
        when (schema.typeDefinition(name)) {
          is GQLScalarTypeDefinition -> IrScalarType(name)
          is GQLEnumTypeDefinition -> {
            IrEnumType(name = name)
          }
          is GQLInputObjectTypeDefinition -> {
            IrInputObjectType(name)
          }
          is GQLObjectTypeDefinition -> {
            IrModelType(MODEL_UNKNOWN)
          }
          is GQLInterfaceTypeDefinition -> {
            IrModelType(MODEL_UNKNOWN)
          }
          is GQLUnionTypeDefinition -> {
            IrModelType(MODEL_UNKNOWN)
          }
        }
      }
    }
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

      val description: String?,
      val type: GQLType,
      val deprecationReason: String?,
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

  override fun merge(fields: List<FieldWithParent>): List<MergedField> {
    return fields.map { fieldWithParent ->
      val gqlField = fieldWithParent.gqlField
      val typeDefinition = schema.typeDefinition(fieldWithParent.parentType)
      val fieldDefinition = gqlField.definitionFromScope(schema, typeDefinition)

      check(fieldDefinition != null) {
        "cannot find field definition for field '${gqlField.responseName()}' of type '${typeDefinition.name}'"
      }
      val forceNonNull = gqlField.directives.findNonnull() || typeDefinition.isFieldNonNull(gqlField.name)

      CollectedField(
          name = gqlField.name,
          alias = gqlField.alias,
          condition = gqlField.directives.toBooleanExpression(),
          selections = gqlField.selectionSet?.selections ?: emptyList(),
          type = fieldDefinition.type,
          description = fieldDefinition.description,
          deprecationReason = fieldDefinition.directives.findDeprecationReason(),
          forceNonNull = forceNonNull,
          forceOptional = gqlField.directives.optionalValue() == true,
          parentType = fieldWithParent.parentType
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


      val info = IrFieldInfo(
          responseName = first.alias ?: first.name,
          description = description,
          deprecationReason = deprecationReason,
          type = irType,
          gqlType = first.type
      )

      MergedField(
          info = info,
          condition = BooleanExpression.Or(fieldsWithSameResponseName.map { it.condition }.toSet()).simplify(),
          selections = childSelections,
          rawTypeName = first.type.leafType().name,
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
internal fun List<GQLDirective>.toBooleanExpression(): BooleanExpression<BVariable> {
  val conditions = mapNotNull {
    it.toBooleanExpression()
  }
  return if (conditions.isEmpty()) {
    BooleanExpression.True
  } else {
    check(conditions.toSet().size == conditions.size) {
      "Apollo: duplicate @skip/@include directives are not allowed"
    }
    // Having both @skip and @include is allowed
    // In that case, it's equivalent to a "And"
    // See https://spec.graphql.org/draft/#sec--include
    BooleanExpression.And(conditions.toSet()).simplify()
  }
}

internal fun GQLDirective.toBooleanExpression(): BooleanExpression<BVariable>? {
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
    is GQLVariableValue -> BooleanExpression.Element(BVariable(name = value.name)).let {
      if (name == "skip") not(it) else it
    }
    else -> throw IllegalStateException("Apollo: cannot pass ${value.toUtf8()} to '$name' directive")
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

internal fun IrType.replacePlaceholder(newPath: String): IrType {
  return when (this) {
    is IrNonNullType -> IrNonNullType(ofType = ofType.replacePlaceholder(newPath))
    is IrListType -> IrListType(ofType = ofType.replacePlaceholder(newPath))
    is IrModelType -> copy(path = newPath)
    else -> error("Not a compound type?")
  }
}
