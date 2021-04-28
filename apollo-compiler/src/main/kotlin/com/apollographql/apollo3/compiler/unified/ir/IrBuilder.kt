package com.apollographql.apollo3.compiler.unified.ir

import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.MetadataFragment
import com.apollographql.apollo3.compiler.frontend.GQLArgument
import com.apollographql.apollo3.compiler.frontend.GQLBooleanValue
import com.apollographql.apollo3.compiler.frontend.GQLDirective
import com.apollographql.apollo3.compiler.frontend.GQLEnumTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLEnumValue
import com.apollographql.apollo3.compiler.frontend.GQLEnumValueDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFieldDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFloatValue
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLInputValueDefinition
import com.apollographql.apollo3.compiler.frontend.GQLIntValue
import com.apollographql.apollo3.compiler.frontend.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLListType
import com.apollographql.apollo3.compiler.frontend.GQLListValue
import com.apollographql.apollo3.compiler.frontend.GQLNamedType
import com.apollographql.apollo3.compiler.frontend.GQLNonNullType
import com.apollographql.apollo3.compiler.frontend.GQLNullValue
import com.apollographql.apollo3.compiler.frontend.GQLObjectTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLObjectValue
import com.apollographql.apollo3.compiler.frontend.GQLOperationDefinition
import com.apollographql.apollo3.compiler.frontend.GQLScalarTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLSelection
import com.apollographql.apollo3.compiler.frontend.GQLStringValue
import com.apollographql.apollo3.compiler.frontend.GQLType
import com.apollographql.apollo3.compiler.frontend.GQLUnionTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLValue
import com.apollographql.apollo3.compiler.frontend.GQLVariableDefinition
import com.apollographql.apollo3.compiler.frontend.GQLVariableValue
import com.apollographql.apollo3.compiler.frontend.InputValueScope
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.coerce
import com.apollographql.apollo3.compiler.frontend.definitionFromScope
import com.apollographql.apollo3.compiler.frontend.findDeprecationReason
import com.apollographql.apollo3.compiler.frontend.findOptional
import com.apollographql.apollo3.compiler.frontend.inferVariables
import com.apollographql.apollo3.compiler.frontend.leafType
import com.apollographql.apollo3.compiler.frontend.pretty
import com.apollographql.apollo3.compiler.frontend.responseName
import com.apollographql.apollo3.compiler.frontend.rootTypeDefinition
import com.apollographql.apollo3.compiler.frontend.toUtf8
import com.apollographql.apollo3.compiler.frontend.toUtf8WithIndents

class IrBuilder(
    private val schema: Schema,
    private val operationDefinitions: List<GQLOperationDefinition>,
    private val fragmentDefinitions: List<GQLFragmentDefinition>,
    private val alwaysGenerateTypesMatching: Set<String>,
    private val customScalarToKotlinName: Map<String, String>,
    private val metadataFragments: List<MetadataFragment>,
    private val metadataEnums: Set<String>,
    private val metadataInputObjects: Set<String>,
    private val generateFragmentAsInterfaces: Boolean,
    private val metadataSchema: Boolean,
) : FieldMerger {
  private val allGQLFragmentDefinitions = (metadataFragments.map { it.definition } + fragmentDefinitions).associateBy { it.name }

  private val usedEnums = mutableSetOf<String>()
  private val inputObjectsToGenerate = mutableListOf<String>()

  private val irFieldBuilder = IrRootFieldBuilder(
      schema = schema,
      allGQLFragmentDefinitions = allGQLFragmentDefinitions,
      fieldMerger = this,
  )

  private val allFragmentFields = allGQLFragmentDefinitions.map { (name, fragmentDefinition) ->
    name to irFieldBuilder.build(
        selections = fragmentDefinition.selectionSet.selections,
        rawTypeName = fragmentDefinition.typeCondition.name
    )
  }.toMap()

  private val modelGroupsBuilder = IrModelGroupsBuilder(
      allFragmentFields,
      false
  )

  private fun shouldAlwaysGenerate(name: String) = alwaysGenerateTypesMatching.map { Regex(it) }.any { it.matches(name) }

  fun build(): IntermediateRepresentation {
    val operations = operationDefinitions.map { it.toIr() }
    val fragments = allGQLFragmentDefinitions.values.map { it.toIr() }

    val visitedInputObjects = mutableSetOf<String>()

    // Input objects contain (possible reentrant) references so build them before enums as they could add some
    val extraInputObjects = schema.typeDefinitions.values.filterIsInstance<GQLInputObjectTypeDefinition>()
        .map { it.name }
        .filter { shouldAlwaysGenerate(it) }
    inputObjectsToGenerate.addAll(extraInputObjects)
    val inputObjects = mutableListOf<IrInputObject>()
    while (inputObjectsToGenerate.isNotEmpty()) {
      val name = inputObjectsToGenerate.removeAt(0)
      if (visitedInputObjects.contains(name)) {
        continue
      }
      visitedInputObjects.add(name)
      inputObjects.add((schema.typeDefinition(name) as GQLInputObjectTypeDefinition).toIr())
    }

    val extraEnums = schema.typeDefinitions.values.filterIsInstance<GQLEnumTypeDefinition>()
        .map { it.name }
        .filter { shouldAlwaysGenerate(it) }
    val enums = (usedEnums + extraEnums).map { name ->
      (schema.typeDefinition(name) as GQLEnumTypeDefinition).toIr()
    }

    val customScalars = schema.typeDefinitions.values
        .filterIsInstance<GQLScalarTypeDefinition>()
        .filter { !it.isBuiltIn() }
        .map { it.toIr() }

    val objects = schema.typeDefinitions.values
        .filterIsInstance<GQLObjectTypeDefinition>()
        .filter { !it.isBuiltIn() }
        .map { it.toIr() }

    val interfaces = schema.typeDefinitions.values
        .filterIsInstance<GQLInterfaceTypeDefinition>()
        .filter { !it.isBuiltIn() }
        .map { it.toIr() }

    val unions = schema.typeDefinitions.values
        .filterIsInstance<GQLUnionTypeDefinition>()
        .filter { !it.isBuiltIn() }
        .map { it.toIr() }

    return IntermediateRepresentation(
        operations = operations,
        // TODO: multi-module
        fragments = fragments,
        inputObjects = inputObjects,
        enums = enums,
        customScalars = customScalars,
        objects = objects,
        interfaces = interfaces,
        unions = unions,
        metadataFragments = metadataFragments,
        metadataEnums = metadataEnums,
        metadataInputObjects = metadataInputObjects,
        metadataSchema = metadataSchema
    )
  }


  private fun GQLObjectTypeDefinition.toIr(): IrObject {
    return IrObject(
        name = name,
        implements = implementsInterfaces,
        description = description,
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLInterfaceTypeDefinition.toIr(): IrInterface {
    return IrInterface(
        name = name,
        implements = implementsInterfaces,
        description = description,
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLUnionTypeDefinition.toIr(): IrUnion {
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
        kotlinName = customScalarToKotlinName[name],
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
    val coercedDefaultValue = defaultValue?.coerce(type, schema)?.orThrow()

    var irType = type.toIr()
    if (type !is GQLNonNullType || coercedDefaultValue != null) {
      /**
       * Contrary to [IrVariable], we default to making input fields optional as they are out of control of the user and
       * we don't want to force users to fill input all values to define an input object
      */
      irType = IrOptionalType(irType)
    }
    return IrInputField(
        name = name,
        description = description,
        deprecationReason = directives.findDeprecationReason(),
        type = irType,
        defaultValue = coercedDefaultValue?.toIr(),
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

  private fun GQLOperationDefinition.toIr(): IrOperation {
    val typeDefinition = this.rootTypeDefinition(schema)
        ?: throw IllegalStateException("ApolloGraphql: cannot find root type for '$operationType'")

    check(name != null) {
      "Apollo doesn't support anonymous operation."
    }

    val builder = IrRootFieldBuilder(
        schema = schema,
        allGQLFragmentDefinitions = allGQLFragmentDefinitions,
        fieldMerger = this@IrBuilder,
    )

    val dataField = builder.build(
        selections = selectionSet.selections,
        rawTypeName = typeDefinition.name,
    )

    val sourceWithFragments = (toUtf8WithIndents() + "\n" + builder.collectedFragments.joinToString(
        separator = "\n"
    ) { fragmentName ->
      allGQLFragmentDefinitions[fragmentName]!!.toUtf8WithIndents()
    }).trimEnd('\n')

    val result = modelGroupsBuilder.buildOperationModelGroups(
        field = dataField,
        operationName = name
    )
    return IrOperation(
        name = name,
        description = description,
        operationType = IrOperationType.valueOf(operationType.capitalize()),
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        field = dataField,
        sourceWithFragments = sourceWithFragments,
        filePath = sourceLocation.filePath!!,
        dataModelId = result.rootModelId,
        modelGroups = result.modelGroups
    )
  }


  private fun GQLFragmentDefinition.toIr(): IrNamedFragment {
    val typeDefinition = schema.typeDefinition(typeCondition.name)

    val variableDefinitions = inferVariables(schema, allGQLFragmentDefinitions)

    val field = allFragmentFields[name] ?: error("Cannot find fragment $name")

    val interfaceResult = modelGroupsBuilder.buildFragmentInterfaceGroups(name)
    val implementationResult = modelGroupsBuilder.buildFragmentImplementationGroups(name)

    return IrNamedFragment(
        name = name,
        description = description,
        filePath = sourceLocation.filePath,
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        field = field,
        interfaceId = interfaceResult.rootModelId,
        interfaceModelGroups = interfaceResult.modelGroups,
        implementationId = implementationResult.rootModelId,
        implementationModelGroups = implementationResult.modelGroups,
    )
  }

  private fun InputValueScope.VariableReference.toIr(): IrVariable {
    var type = expectedType.toIr()
    // This is an inferred variable from a fragment
    if (expectedType !is GQLNonNullType) {
      type = IrOptionalType(type)
    }
    return IrVariable(
        name = this.variable.name,
        defaultValue = null,
        type = type,
    )
  }

  private fun GQLVariableDefinition.toIr(): IrVariable {
    val coercedDefaultValue = defaultValue?.coerce(type, schema)?.orThrow()

    var irType = type.toIr()
    // By default the GraphQL spec treats nullable types as optional variables but most of the times
    // users writing query variables want to give them a value so default to making them non-optional
    // and only make them optional if there is a defaultValue (which means the user has a use case for
    // not sending the value) or if it's opt-in explicitely through the @optional directive
    if (coercedDefaultValue != null || directives.findOptional()) {
      irType = IrOptionalType(irType)
    }
    return IrVariable(
        name = name,
        defaultValue = coercedDefaultValue?.toIr(),
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
      is GQLNamedType -> when (schema.typeDefinition(name)) {
        is GQLScalarTypeDefinition -> {
          when (name) {
            "String" -> IrStringType
            "Boolean" -> IrBooleanType
            "Int" -> IrIntType
            "Float" -> IrFloatType
            "ID" -> IrIdType
            else -> {
              if (customScalarToKotlinName[name] != null) {
                IrCustomScalarType(name)
              } else {
                IrAnyType
              }
            }
          }
        }
        is GQLEnumTypeDefinition -> {
          usedEnums.add(name)
          IrEnumType(name = name)
        }
        is GQLInputObjectTypeDefinition -> {
          inputObjectsToGenerate.add(name)
          IrInputObjectType(name)
        }
        is GQLObjectTypeDefinition,
        is GQLInterfaceTypeDefinition,
        is GQLUnionTypeDefinition,
        -> IrModelType(IrUnknownModelId)
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

  override fun merge(fields: List<FieldWithParent>): List<MergedField> {
    return fields.map { fieldWithParent ->
      val gqlField = fieldWithParent.gqlField
      val typeDefinition = schema.typeDefinition(fieldWithParent.parentType)
      val fieldDefinition = gqlField.definitionFromScope(schema, typeDefinition)

      check(fieldDefinition != null) {
        "cannot find field definition for field '${gqlField.responseName()}' of type '${typeDefinition.name}'"
      }
      CollectedField(
          name = gqlField.name,
          alias = gqlField.alias,
          arguments = gqlField.arguments?.arguments?.map { it.toIr(fieldDefinition) } ?: emptyList(),
          condition = gqlField.directives.toBooleanExpression(),
          selections = gqlField.selectionSet?.selections ?: emptyList(),
          type = fieldDefinition.type,
          description = fieldDefinition.description,
          deprecationReason = fieldDefinition.directives.findDeprecationReason(),
      )
    }.groupBy {
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

      val info = IrFieldInfo(
          alias = first.alias,
          name = first.name,
          arguments = first.arguments,
          description = first.description,
          deprecationReason = first.deprecationReason,
          type = first.type.toIr(),
          rawTypeName = first.type.leafType().name
      )

      MergedField(
          info = info,
          condition = BooleanExpression.Or(fieldsWithSameResponseName.map { it.condition }.toSet()).simplify(),
          selections = childSelections,
      )
    }
  }

  private fun GQLArgument.toIr(fieldDefinition: GQLFieldDefinition): IrArgument {
    val argumentDefinition = fieldDefinition.arguments.first { it.name == name }

    return IrArgument(
        name = name,
        value = value.coerce(argumentDefinition.type, schema).orThrow().toIr(),
        defaultValue = argumentDefinition.defaultValue?.coerce(argumentDefinition.type, schema)?.orThrow()?.toIr(),
        type = argumentDefinition.type.toIr()
    )
  }
}

internal fun GQLValue.toIr(): IrValue {
  return when (this) {
    is GQLIntValue -> IrIntValue(value = value)
    is GQLStringValue -> IrStringValue(value = value)
    is GQLFloatValue -> IrFloatValue(value = value)
    is GQLBooleanValue -> IrBooleanValue(value = value)
    is GQLEnumValue -> IrEnumValue(value = value)
    is GQLNullValue -> IrNullValue
    is GQLVariableValue -> IrVariableValue(name = name)
    is GQLListValue -> IrListValue(values = values.map { it.toIr() })
    is GQLObjectValue -> IrObjectValue(
        fields = fields.map {
          IrObjectValue.Field(name = it.name, value = it.value.toIr())
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
internal fun List<GQLDirective>.toBooleanExpression(): BooleanExpression {
  val conditions = mapNotNull {
    it.toBooleanExpression()
  }
  return if (conditions.isEmpty()) {
    BooleanExpression.True
  } else {
    check(conditions.toSet().size == conditions.size) {
      "ApolloGraphQL: duplicate @skip/@include directives are not allowed"
    }
    // Having both @skip and @include is allowed
    // In that case, it's equivalent to a "And"
    // See https://spec.graphql.org/draft/#sec--include
    BooleanExpression.And(conditions.toSet()).simplify()
  }
}

internal fun GQLDirective.toBooleanExpression(): BooleanExpression? {
  if (setOf("skip", "include").contains(name).not()) {
    // not a condition directive
    return null
  }
  if (arguments?.arguments?.size != 1) {
    throw IllegalStateException("ApolloGraphQL: wrong number of arguments for '$name' directive: ${arguments?.arguments?.size}")
  }

  val argument = arguments.arguments.first()

  return when (val value = argument.value) {
    is GQLBooleanValue -> {
      if (value.value) BooleanExpression.True else BooleanExpression.False
    }
    is GQLVariableValue -> BooleanExpression.Variable(
        name = value.name,
    ).let {
      if (name == "skip") it.not() else it
    }
    else -> throw IllegalStateException("ApolloGraphQL: cannot pass ${value.toUtf8()} to '$name' directive")
  }
}
