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
import com.apollographql.apollo3.ast.GQLEnumValueDefinition
import com.apollographql.apollo3.ast.GQLFieldDefinition
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
import com.apollographql.apollo3.ast.InferredVariable
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.Schema.Companion.TYPE_POLICY
import com.apollographql.apollo3.ast.TransformResult
import com.apollographql.apollo3.ast.coerceInExecutableContextOrThrow
import com.apollographql.apollo3.ast.coerceInSchemaContextOrThrow
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.findNonnull
import com.apollographql.apollo3.ast.findOptInFeature
import com.apollographql.apollo3.ast.findTargetName
import com.apollographql.apollo3.ast.inferVariables
import com.apollographql.apollo3.ast.internal.toConnectionFields
import com.apollographql.apollo3.ast.internal.toEmbeddedFields
import com.apollographql.apollo3.ast.isFieldNonNull
import com.apollographql.apollo3.ast.rawType
import com.apollographql.apollo3.ast.optionalValue
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.responseName
import com.apollographql.apollo3.ast.rootTypeDefinition
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.transform
import com.apollographql.apollo3.compiler.MODELS_COMPAT
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED_WITH_INTERFACES
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.ScalarInfo

internal class IrBuilder(
    private val schema: Schema,
    private val operationDefinitions: List<GQLOperationDefinition>,
    private val alwaysGenerateResponseBasedDataModelGroup: Boolean,
    private val fragmentDefinitions: List<GQLFragmentDefinition>,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val alwaysGenerateTypesMatching: Set<String>,
    private val scalarMapping: Map<String, ScalarInfo>,
    private val codegenModels: String,
    private val generateOptionalOperationVariables: Boolean,
    private val generateDataBuilders: Boolean,
    private val fieldsOnDisjointTypesMustMerge: Boolean,
) : FieldMerger {
  private val usedTypes = mutableListOf<String>()

  private val responseBasedBuilder = ResponseBasedModelGroupBuilder(
      schema,
      allFragmentDefinitions,
      this
  )

  private val builder = when (codegenModels) {
    @Suppress("DEPRECATION")
    MODELS_COMPAT,
    -> OperationBasedModelGroupBuilder(
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
    MODELS_OPERATION_BASED_WITH_INTERFACES -> OperationBasedWithInterfacesModelGroupBuilder(
        schema = schema,
        allFragmentDefinitions = allFragmentDefinitions,
        fieldMerger = this,
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

    // Inject extra types
    usedTypes.addAll(schema.typeDefinitions.keys.filter { shouldAlwaysGenerate(it) })

    // Inject all built-in scalars
    usedTypes.add("String")
    usedTypes.add("Boolean")
    usedTypes.add("Int")
    usedTypes.add("Float")
    usedTypes.add("ID")

    // Inject custom scalars specified in the Gradle configuration
    usedTypes.addAll(scalarMapping.keys)

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

    // Input objects and Interfaces contain (possible reentrant) references, so we need to loop here
    while (usedTypes.isNotEmpty()) {
      val name = usedTypes.removeAt(0)
      if (visitedTypes.contains(name)) {
        continue
      }
      visitedTypes.add(name)
      val typeDefinition = schema.typeDefinition(name)
      if (typeDefinition.isBuiltIn() && typeDefinition !is GQLScalarTypeDefinition) {
        // We don't generate builtin types, except scalars
        continue
      }

      when (typeDefinition) {
        is GQLEnumTypeDefinition -> enums.add(typeDefinition.toIr())
        is GQLObjectTypeDefinition -> {
          objects.add(typeDefinition.toIr())
          if (generateDataBuilders) {
            // Add all direct super types because they are used in the MapTypes hierarchy
            // class HumanMap(map: Map<String, Any?>): CharacterMap, Map<String, Any?> by map
            usedTypes.addAll(typeDefinition.implementsInterfaces)
            schema.typeDefinitions.values.filterIsInstance<GQLUnionTypeDefinition>().forEach {
              if (it.memberTypes.map { it.name }.contains(typeDefinition.name)) {
                usedTypes.add(it.name)
              }
            }
            // Add all fields types
            typeDefinition.fields.forEach {
              usedTypes.add(it.type.rawType().name)
            }
          }
        }
        is GQLUnionTypeDefinition -> {
          unions.add(typeDefinition.toIr())
          if (generateDataBuilders) {
            usedTypes.addAll(typeDefinition.memberTypes.map { it.name })
          }
        }
        is GQLInterfaceTypeDefinition -> {
          interfaces.add(typeDefinition.toIr())
          if (generateDataBuilders) {
            // Add all direct super types
            usedTypes.addAll(typeDefinition.implementsInterfaces)

            // Add all direct sub types because the user might want to use any of them
            // GetHeroQuery.Data {
            //   hero = buildHuman {}  // or buildDroid {}
            // }
            schema.typeDefinitions.values.forEach {
              if (it is GQLInterfaceTypeDefinition && it.implementsInterfaces.contains(typeDefinition.name)) {
                usedTypes.add(it.name)
              }
              if (it is GQLObjectTypeDefinition && it.implementsInterfaces.contains(typeDefinition.name)) {
                usedTypes.add(it.name)
              }
            }
            // Add all fields types
            typeDefinition.fields.forEach {
              usedTypes.add(it.type.rawType().name)
            }
          }
        }
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
        connectionTypes = schema.connectionTypes.toList()
    )
  }

  private fun GQLObjectTypeDefinition.toIr(): IrObject {
    // Needed to build the compiled type
    usedTypes.addAll(implementsInterfaces)

    return IrObject(
        name = name,
        targetName = directives.findTargetName(schema),
        implements = implementsInterfaces,
        keyFields = schema.keyFields(name).toList(),
        description = description,
        // XXX: this is not spec-compliant. Directive cannot be on object definitions
        deprecationReason = directives.findDeprecationReason(),
        embeddedFields = directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toEmbeddedFields() +
            directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toConnectionFields() +
            connectionTypeEmbeddedFields(name, schema),
        mapProperties = fields.map {
          it.toIrMapProperty()
        },
        superTypes = schema.superTypes(this).toList()
    )
  }

  private fun GQLFieldDefinition.toIrMapProperty(): IrMapProperty {
    return IrMapProperty(
        name,
        type.toIrType2()
    )
  }

  private fun GQLType.toIrType2(): IrType2 {
    return when(this) {
      is GQLNonNullType -> IrNonNullType2(type.toIrType2())
      is GQLListType -> IrListType2(type.toIrType2())
      is GQLNamedType -> {
        val typeDefinition = schema.typeDefinition(name)
        when(typeDefinition) {
          is GQLScalarTypeDefinition -> return IrScalarType2(name)
          is GQLEnumTypeDefinition -> return IrEnumType2(name)
          is GQLInputObjectTypeDefinition -> error("Input objects are not supported in data builders")
          is GQLInterfaceTypeDefinition,
          is GQLObjectTypeDefinition,
          is GQLUnionTypeDefinition -> IrCompositeType2(name)
        }
      }
    }
  }

  private fun GQLInterfaceTypeDefinition.toIr(): IrInterface {
    // Needed to build the compiled type
    usedTypes.addAll(implementsInterfaces)

    return IrInterface(
        name = name,
        targetName = directives.findTargetName(schema),
        implements = implementsInterfaces,
        keyFields = schema.keyFields(name).toList(),
        description = description,
        // XXX: this is not spec-compliant. Directive cannot be on interfaces
        deprecationReason = directives.findDeprecationReason(),
        embeddedFields = directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toEmbeddedFields() +
            directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toConnectionFields() +
            connectionTypeEmbeddedFields(name, schema),
    )
  }

  /**
   * If [typeName] is declared as a Relay Connection type (via the `@typePolicy` directive), return the standard arguments
   * to be embedded.
   * Otherwise, return an empty set.
   */
  private fun connectionTypeEmbeddedFields(typeName: String, schema: Schema): Set<String> {
    return if (typeName in schema.connectionTypes) {
      setOf("edges")
    } else {
      emptySet()
    }
  }

  private fun GQLUnionTypeDefinition.toIr(): IrUnion {
    // Needed to build the compiled type
    usedTypes.addAll(memberTypes.map { it.name })

    return IrUnion(
        name = name,
        targetName = directives.findTargetName(schema),
        members = memberTypes.map { it.name },
        description = description,
        // XXX: this is not spec-compliant. Directive cannot be on union definitions
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLScalarTypeDefinition.toIr(): IrCustomScalar {
    return IrCustomScalar(
        name = name,
        targetName = directives.findTargetName(schema),
        kotlinName = scalarMapping[name]?.targetName,
        description = description,
        // XXX: this is not spec-compliant. Directive cannot be on scalar definitions
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLInputObjectTypeDefinition.toIr(): IrInputObject {
    return IrInputObject(
        name = name,
        targetName = directives.findTargetName(schema),
        description = description,
        // XXX: this is not spec-compliant. Directive cannot be on input objects definitions
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
       * Contrary to [IrVariable], we default to making input fields optional as they are out of control of the user, and
       * we don't want to force users to fill all values to define an input object
       */
      irType = irType.makeOptional()
    }
    return IrInputField(
        name = name,
        description = description,
        deprecationReason = directives.findDeprecationReason(),
        optInFeature = directives.findOptInFeature(schema),
        type = irType,
        defaultValue = coercedDefaultValue?.toIrValue(),
    )
  }

  private fun GQLEnumTypeDefinition.toIr(): IrEnum {
    return IrEnum(
        name = name,
        targetName = directives.findTargetName(schema),
        description = description,
        values = enumValues.map { it.toIr() }
    )
  }

  private fun GQLEnumValueDefinition.toIr(): IrEnum.Value {
    return IrEnum.Value(
        name = name,
        targetName = directives.findTargetName(schema) ?: name,
        description = description,
        deprecationReason = directives.findDeprecationReason(),
        optInFeature = directives.findOptInFeature(schema),
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

    val responseBasedModelGroup = when {
      codegenModels == MODELS_RESPONSE_BASED -> dataModelGroup
      alwaysGenerateResponseBasedDataModelGroup -> responseBasedBuilder.buildOperationData(
          selections = selectionSet.selections,
          rawTypeName = typeDefinition.name,
          operationName = name!!
      ).second
      else -> null
    }

    return IrOperation(
        name = name!!,
        description = description,
        operationType = operationType.toIrOperationType(schema.rootTypeNameFor(operationType)),
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        selectionSets = SelectionSetsBuilder(schema, allFragmentDefinitions).build(selectionSet.selections, typeDefinition.name),
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

    return IrFragmentDefinition(
        name = name,
        description = description,
        filePath = sourceLocation.filePath!!,
        typeCondition = typeDefinition.name,
        variables = inferredVariables.map { it.toIr() },
        selectionSets = SelectionSetsBuilder(schema, allFragmentDefinitions).build(selectionSet.selections, typeCondition.name),
        interfaceModelGroup = interfaceModelGroup,
        dataProperty = dataProperty,
        dataModelGroup = dataModelGroup,
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
      irType !is IrNonNullType -> {
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
   * Maps to [IrType] and also keep tracks of what types are actually used, so we only generate those
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

  override fun merge(fields: List<FieldWithParent>): List<MergedField> {
    return fields.map { fieldWithParent ->
      val gqlField = fieldWithParent.gqlField
      val typeDefinition = schema.typeDefinition(fieldWithParent.parentType)
      val fieldDefinition = gqlField.definitionFromScope(schema, typeDefinition)

      check(fieldDefinition != null) {
        "cannot find field definition for field '${gqlField.responseName()}' of type '${typeDefinition.name}'"
      }
      val forceNonNull = gqlField.directives.findNonnull(schema) || typeDefinition.isFieldNonNull(gqlField.name, schema)

      CollectedField(
          name = gqlField.name,
          alias = gqlField.alias,
          condition = gqlField.directives.toIncludeBooleanExpression(),
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
      val optInFeature = fieldsWithSameResponseName.associateBy { it.optInFeature }.values.let { experimentalCanditates ->
        if (experimentalCanditates.size == 1) {
          experimentalCanditates.single().optInFeature
        } else {
          val parents = experimentalCanditates.filter { it.optInFeature != null }.map { it.parentType }
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
internal fun List<GQLDirective>.toIncludeBooleanExpression(): BooleanExpression<BVariable> {
  val conditions = mapNotNull {
    it.toIncludeBooleanExpression()
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

internal fun GQLDirective.toIncludeBooleanExpression(): BooleanExpression<BVariable>? {
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
    is GQLVariableValue -> BooleanExpression.Element(BVariable(name = value.name))
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
  return toIncludeBooleanExpression().and(deferBooleanExpression).simplify()
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

internal fun IrType.replacePlaceholder(newPath: String): IrType {
  return when (this) {
    is IrNonNullType -> IrNonNullType(ofType = ofType.replacePlaceholder(newPath))
    is IrListType -> IrListType(ofType = ofType.replacePlaceholder(newPath))
    is IrModelType -> copy(path = newPath)
    else -> error("Not a compound type?")
  }
}

internal fun IrType.replacePath(transform: (String) -> String): IrType {
  return when (this) {
    is IrNonNullType -> IrNonNullType(ofType = ofType.replacePath(transform))
    is IrListType -> IrListType(ofType = ofType.replacePath(transform))
    is IrModelType -> copy(path = transform(path))
    else -> error("Not a compound type?")
  }
}
