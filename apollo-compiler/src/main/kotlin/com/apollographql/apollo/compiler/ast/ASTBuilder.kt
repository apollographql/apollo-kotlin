package com.apollographql.apollo.compiler.ast

import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.codegen.kotlin.normalizeJsonValue
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.*
import com.apollographql.apollo.compiler.singularize

fun CodeGenerationIR.toAST(
  customTypeMap: Map<String, String>,
  typesPackageName: String,
  fragmentsPackage: String,
  useSemanticNaming: Boolean
): AST {
  val enums = typesUsed.filter { it.kind == TypeDeclaration.KIND_ENUM }.map { it.asASTEnumType() }
  val inputTypes = typesUsed.filter { it.kind == TypeDeclaration.KIND_INPUT_OBJECT_TYPE }.map {
    it.asASTInputType(
      enums = enums,
      customTypeMap = customTypeMap,
      typesPackageName = typesPackageName
    )
  }
  val irFragments = fragments.associate { it.fragmentName to it }
  val fragments = fragments.map {
    it.toASTFragment(
      Context(
        reservedObjectTypeRef = null,
        customTypeMap = customTypeMap,
        enums = enums,
        typesPackageName = typesPackageName,
        fragmentsPackage = fragmentsPackage,
        fragments = irFragments
      )
    )
  }
  val operations = operations.map { operation ->
    operation.asASTOperation(
      operationClassName = operation.normalizedOperationName(useSemanticNaming).capitalize(),
      context = Context(
        reservedObjectTypeRef = AST.TypeRef(name = operation.normalizedOperationName(useSemanticNaming).capitalize()),
        customTypeMap = customTypeMap,
        enums = enums,
        typesPackageName = typesPackageName,
        fragmentsPackage = fragmentsPackage,
        fragments = irFragments
      )
    )
  }
  return AST(
    enums = enums,
    customTypes = customTypeMap,
    inputTypes = inputTypes,
    fragments = fragments,
    operations = operations
  )
}

private fun TypeDeclaration.asASTEnumType() = AST.EnumType(
  name = name.capitalize().escapeKotlinReservedWord(),
  description = description ?: "",
  values = values?.map { value ->
    AST.EnumType.Value(
      constName = value.name.toUpperCase().escapeKotlinReservedWord(),
      value = value.name,
      description = value.description ?: "",
      isDeprecated = value.isDeprecated ?: false,
      deprecationReason = value.deprecationReason ?: ""
    )
  } ?: emptyList()
)

private fun TypeDeclaration.asASTInputType(
  enums: List<AST.EnumType>,
  customTypeMap: Map<String, String>,
  typesPackageName: String
) = AST.InputType(
  name = name.capitalize().escapeKotlinReservedWord(),
  description = description ?: "",
  fields = fields?.map { field ->
    val inputFieldType = resolveFieldType(
      graphQLType = field.type,
      enums = enums,
      customTypeMap = customTypeMap,
      typesPackageName = typesPackageName
    )
    AST.InputType.Field(
      name = field.name.decapitalize().escapeKotlinReservedWord(),
      schemaName = field.name,
      type = inputFieldType,
      description = field.description,
      isOptional = !field.type.endsWith("!"),
      defaultValue = if (inputFieldType.isCustomType) null else field.defaultValue?.normalizeJsonValue(field.type)
    )
  } ?: emptyList()
)

private fun Operation.asASTOperation(
  context: Context,
  operationClassName: String
): AST.OperationType {
  val dataTypeRef = context.addObjectType(typeName = "Data", packageName = operationClassName) {
    AST.ObjectType(
      className = "Data",
      schemaName = "Data",
      fields = fields.map { it.asASTField(context) },
      fragmentsType = null
    )
  }
  return AST.OperationType(
    name = operationClassName,
    type = astOperationType,
    definition = source,
    operationId = operationId,
    queryDocument = sourceWithFragments,
    variables = AST.InputType(
      name = "Variables",
      description = "",
      fields = variables.map { variable ->
        AST.InputType.Field(
          name = variable.name.decapitalize().escapeKotlinReservedWord(),
          schemaName = variable.name,
          type = resolveFieldType(
            graphQLType = variable.type,
            enums = context.enums,
            customTypeMap = context.customTypeMap,
            typesPackageName = context.typesPackageName
          ),
          isOptional = variable.optional(),
          defaultValue = null,
          description = ""
        )
      }
    ),
    data = dataTypeRef,
    nestedObjects = context.objectTypes,
    filePath = filePath
  )
}

private fun Field.asASTField(context: Context): AST.ObjectType.Field {
  return when {
    isArrayTypeField -> asASTArrayField(context)
    isObjectTypeField -> asASTObjectField(context)
    else -> toASTScalarField(context)
  }
}

private fun Field.asASTArrayField(context: Context): AST.ObjectType.Field {
  val fieldType = if (fields?.isNotEmpty() == true) {
    val objectType = AST.FieldType.Object(
      context.addObjectType(
        type = responseName.replace("[", "").replace("[", "").replace("!", ""),
        schemaType = type.replace("[", "").replace("[", "").replace("!", ""),
        fragmentSpreads = fragmentSpreads ?: emptyList(),
        inlineFragments = inlineFragments ?: emptyList(),
        fields = fields
      )
    )
    var result: AST.FieldType.Array = AST.FieldType.Array(rawType = objectType)
    repeat(type.count { it == '[' } - 1) {
      result = AST.FieldType.Array(rawType = result)
    }
    result
  } else {
    resolveFieldType(
      graphQLType = type,
      enums = context.enums,
      customTypeMap = context.customTypeMap,
      typesPackageName = context.typesPackageName
    )
  }
  return AST.ObjectType.Field(
    name = responseName.decapitalize().escapeKotlinReservedWord(),
    responseName = responseName,
    schemaName = fieldName,
    type = fieldType,
    description = description ?: "",
    isOptional = !type.endsWith("!") || isConditional,
    isDeprecated = isDeprecated ?: false,
    deprecationReason = deprecationReason ?: "",
    arguments = args?.associate { it.name to it.value.normalizeJsonValue(it.type) } ?: emptyMap(),
    conditions = normalizedConditions
  )
}

private fun Field.asASTObjectField(context: Context): AST.ObjectType.Field {
  val typeRef = context.addObjectType(
    type = responseName.replace("[", "").replace("[", "").replace("!", ""),
    schemaType = type.replace("[", "").replace("[", "").replace("!", ""),
    fragmentSpreads = fragmentSpreads ?: emptyList(),
    inlineFragments = inlineFragments ?: emptyList(),
    fields = fields ?: emptyList()
  )
  return AST.ObjectType.Field(
    name = responseName.decapitalize().escapeKotlinReservedWord(),
    responseName = responseName,
    schemaName = fieldName,
    type = AST.FieldType.Object(typeRef),
    description = description ?: "",
    isOptional = !type.endsWith("!") || isConditional || (inlineFragments?.isNotEmpty() == true),
    isDeprecated = isDeprecated ?: false,
    deprecationReason = deprecationReason ?: "",
    arguments = args?.associate { it.name to it.value.normalizeJsonValue(it.type) } ?: emptyMap(),
    conditions = normalizedConditions
  )
}

private fun List<Fragment>.toFragmentsObjectField(
  fragmentsPackage: String,
  isOptional: (typeCondition: String) -> Boolean
): Pair<AST.ObjectType.Field?, AST.ObjectType?> {
  if (isEmpty()) {
    return null to null
  }
  val type = AST.ObjectType(
    className = "Fragments",
    schemaName = "Fragments",
    fields = map { fragment ->
      AST.ObjectType.Field(
        name = fragment.fragmentName.decapitalize().escapeKotlinReservedWord(),
        responseName = fragment.fragmentName,
        schemaName = fragment.fragmentName,
        type = AST.FieldType.Object(AST.TypeRef(
          name = fragment.fragmentName.capitalize(),
          packageName = fragmentsPackage
        )),
        description = "",
        isOptional = isOptional(fragment.typeCondition),
        isDeprecated = false,
        deprecationReason = "",
        arguments = emptyMap(),
        conditions = fragment.possibleTypes.map { AST.ObjectType.Field.Condition.Type(it) }
      )
    },
    fragmentsType = null
  )
  val field = AST.ObjectType.Field(
    name = type.className.decapitalize().escapeKotlinReservedWord(),
    responseName = "__typename",
    schemaName = "__typename",
    type = AST.FieldType.Fragments(
      name = type.className,
      fields = type.fields.map { field ->
        AST.FieldType.Fragments.Field(
          name = field.name,
          type = (field.type as AST.FieldType.Object).typeRef,
          isOptional = field.isOptional
        )
      }
    ),
    description = "",
    isOptional = false,
    isDeprecated = false,
    deprecationReason = "",
    arguments = emptyMap(),
    conditions = emptyList()
  )
  return field to type
}

private fun Context.addInlineFragmentType(inlineFragment: InlineFragment): AST.TypeRef {
  return addObjectType(
    type = "As${inlineFragment.typeCondition}",
    schemaType = inlineFragment.typeCondition,
    fragmentSpreads = inlineFragment.fragmentSpreads ?: emptyList(),
    inlineFragments = emptyList(),
    fields = inlineFragment.fields
  )
}

private fun inlineObjectFields(
  inlineFragments: Map<InlineFragment, AST.TypeRef>,
  isOptional: (typeCondition: String) -> Boolean
): List<AST.ObjectType.Field> {
  return inlineFragments.map { (inlineFragment, typeRef) ->
    AST.ObjectType.Field(
      name = typeRef.name.decapitalize().escapeKotlinReservedWord(),
      responseName = "__typename",
      schemaName = "__typename",
      type = AST.FieldType.InlineFragment(typeRef),
      description = "",
      isOptional = isOptional(inlineFragment.typeCondition),
      isDeprecated = false,
      deprecationReason = "",
      arguments = emptyMap(),
      conditions = inlineFragment.possibleTypes?.map { AST.ObjectType.Field.Condition.Type(it) }
        ?: inlineFragment.typeCondition.let { listOf(AST.ObjectType.Field.Condition.Type(it)) }
    )
  }
}

private fun Field.toASTScalarField(context: Context): AST.ObjectType.Field {
  return AST.ObjectType.Field(
    name = responseName.decapitalize().escapeKotlinReservedWord(),
    responseName = responseName,
    schemaName = fieldName,
    type = resolveFieldType(
      graphQLType = type,
      enums = context.enums,
      customTypeMap = context.customTypeMap,
      typesPackageName = context.typesPackageName
    ),
    description = description ?: "",
    isOptional = !type.endsWith("!") || isConditional,
    isDeprecated = isDeprecated ?: false,
    deprecationReason = deprecationReason ?: "",
    arguments = args?.associate { it.name to it.value.normalizeJsonValue(it.type) } ?: emptyMap(),
    conditions = normalizedConditions
  )
}

private fun Fragment.toASTFragment(context: Context): AST.FragmentType {
  val inlineFragmentFields = inlineFragments
    .associate { it to context.addInlineFragmentType(it) }
    .let { inlineObjectFields(it) { true } }
  return AST.FragmentType(
    name = fragmentName.capitalize().escapeKotlinReservedWord(),
    definition = source,
    possibleTypes = possibleTypes,
    fields = fields.map { it.asASTField(context) } + inlineFragmentFields,
    nestedObjects = context.objectTypes
  )
}

private val Operation.astOperationType
  get() = when {
    isQuery() -> AST.OperationType.Type.QUERY
    isMutation() -> AST.OperationType.Type.MUTATION
    isSubscription() -> AST.OperationType.Type.SUBSCRIPTION
    else -> throw IllegalArgumentException("Unsupported GraphQL operation type: $operationType")
  }

private val Field.isObjectTypeField: Boolean
  get() = isNonScalar()

private val Field.isArrayTypeField: Boolean
  get() = type.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') }

private val AST.FieldType.isCustomType: Boolean
  get() = this is AST.FieldType.Scalar.Custom || (this as? AST.FieldType.Array)?.rawType?.isCustomType ?: false

private fun resolveFieldType(
  graphQLType: String,
  enums: List<AST.EnumType>,
  customTypeMap: Map<String, String>,
  typesPackageName: String
): AST.FieldType {
  val isGraphQLArrayType = graphQLType.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') }
  if (isGraphQLArrayType) {
    return AST.FieldType.Array(
      resolveFieldType(
        graphQLType = graphQLType.removeSuffix("!").removePrefix("[").removeSuffix("]").removeSuffix("!"),
        enums = enums,
        customTypeMap = customTypeMap,
        typesPackageName = typesPackageName
      )
    )
  } else {
    return when (ScalarType.forName(graphQLType.removeSuffix("!"))) {
      is ScalarType.STRING -> AST.FieldType.Scalar.String
      is ScalarType.INT -> AST.FieldType.Scalar.Int
      is ScalarType.BOOLEAN -> AST.FieldType.Scalar.Boolean
      is ScalarType.FLOAT -> AST.FieldType.Scalar.Float
      else -> when {
        enums.find { it.name == graphQLType.removeSuffix("!") } != null -> AST.FieldType.Scalar.Enum(
          AST.TypeRef(
            name = graphQLType.removeSuffix("!").capitalize().escapeKotlinReservedWord(),
            packageName = typesPackageName
          )
        )
        customTypeMap.containsKey(graphQLType.removeSuffix("!")) -> AST.FieldType.Scalar.Custom(
          schemaType = graphQLType.removeSuffix("!"),
          mappedType = customTypeMap[graphQLType.removeSuffix("!")]!!,
          customEnumConst = graphQLType.removeSuffix("!").toUpperCase().escapeKotlinReservedWord(),
          customEnumType = AST.TypeRef(
            name = "CustomType",
            packageName = typesPackageName
          )
        )
        else -> AST.FieldType.Object(
          AST.TypeRef(
            name = graphQLType.removeSuffix("!").capitalize().escapeKotlinReservedWord(),
            packageName = typesPackageName
          )
        )
      }
    }
  }
}

private val Field.normalizedConditions: List<AST.ObjectType.Field.Condition>
  get() {
    return if (isConditional) {
      conditions?.filter { it.kind == Condition.Kind.BOOLEAN.rawValue }?.map {
        AST.ObjectType.Field.Condition.Directive(
          variableName = it.variableName,
          inverted = it.inverted
        )
      } ?: emptyList()
    } else {
      emptyList()
    }
  }

private class Context(
  val reservedObjectTypeRef: AST.TypeRef?,
  val customTypeMap: Map<String, String>,
  val enums: List<AST.EnumType>,
  val typesPackageName: String,
  val fragmentsPackage: String,
  val fragments: Map<String, Fragment>
) {
  private val reservedObjectTypeRefs = HashSet<AST.TypeRef>().applyIf(reservedObjectTypeRef != null) { add(reservedObjectTypeRef!!) }
  private val objectTypeContainer: MutableMap<AST.TypeRef, AST.ObjectType> = LinkedHashMap()
  val objectTypes: Map<AST.TypeRef, AST.ObjectType> = objectTypeContainer

  fun addObjectType(typeName: String, packageName: String = "",
                    provideObjectType: (AST.TypeRef) -> AST.ObjectType): AST.TypeRef {
    val uniqueTypeRef = (reservedObjectTypeRefs).generateUniqueTypeRef(
      typeName = typeName.let { if (it != "Data") it.singularize() else it },
      packageName = packageName
    )
    reservedObjectTypeRefs.add(uniqueTypeRef)
    objectTypeContainer[uniqueTypeRef] = provideObjectType(uniqueTypeRef)
    return uniqueTypeRef
  }

  private fun Set<AST.TypeRef>.generateUniqueTypeRef(typeName: String, packageName: String): AST.TypeRef {
    var index = 0
    var uniqueTypeRef = AST.TypeRef(name = typeName, packageName = packageName)
    while (find { it.name.toLowerCase() == uniqueTypeRef.name.toLowerCase() } != null) {
      uniqueTypeRef = AST.TypeRef(name = "${uniqueTypeRef.name}${++index}", packageName = packageName)
    }
    return uniqueTypeRef
  }
}

private fun Context.addObjectType(
  type: String,
  schemaType: String,
  fragmentSpreads: List<String>,
  inlineFragments: List<InlineFragment>,
  fields: List<Field>
): AST.TypeRef {
  val (fragmentsField, fragmentsObjectType) = fragmentSpreads
    .map { fragments[it] ?: throw IllegalArgumentException("Unable to find fragment definition: $it") }
    .toFragmentsObjectField(fragmentsPackage = fragmentsPackage, isOptional = { it != schemaType.removeSuffix("!") })

  val inlineFragmentFields = inlineFragments
    .associate { it to addInlineFragmentType(it) }
    .let { inlineObjectFields(inlineFragments = it) { it != schemaType.removeSuffix("!") } }

  val normalizedClassName = type.removeSuffix("!").capitalize().escapeKotlinReservedWord()
  return addObjectType(normalizedClassName) { typeRef ->
    AST.ObjectType(
      className = typeRef.name,
      schemaName = type,
      fields = (fields.map { it.asASTField(this) } + inlineFragmentFields).let {
        if (fragmentsField != null) it + fragmentsField else it
      },
      fragmentsType = fragmentsObjectType
    )
  }
}