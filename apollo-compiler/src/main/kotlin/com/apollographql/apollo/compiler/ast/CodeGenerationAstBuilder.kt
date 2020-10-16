package com.apollographql.apollo.compiler.ast

import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.Operation
import com.apollographql.apollo.compiler.ir.Operation.Companion.TYPE_MUTATION
import com.apollographql.apollo.compiler.ir.Operation.Companion.TYPE_QUERY
import com.apollographql.apollo.compiler.ir.Operation.Companion.TYPE_SUBSCRIPTION
import com.apollographql.apollo.compiler.ir.SourceLocation
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.findOperationId
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.resolveType
import com.apollographql.apollo.compiler.ir.Fragment as IrFragment

internal fun CodeGenerationIR.buildCodeGenerationAst(
    schema: IntrospectionSchema,
    customTypeMap: Map<String, String>,
    typesPackageName: String,
    fragmentsPackage: String,
    useSemanticNaming: Boolean,
    operationOutput: OperationOutput
): CodeGenerationAst {
  return CodeGenerationAstBuilder(
      schema = schema,
      customTypeMap = customTypeMap,
      typesPackageName = typesPackageName,
      fragmentsPackage = fragmentsPackage,
      useSemanticNaming = useSemanticNaming,
      operationOutput = operationOutput
  ).build(this.flattenInlineFragments())
}

private class CodeGenerationAstBuilder(
    private val schema: IntrospectionSchema,
    private val customTypeMap: Map<String, String>,
    private val typesPackageName: String,
    private val fragmentsPackage: String,
    private val useSemanticNaming: Boolean,
    private val operationOutput: OperationOutput
) {

  fun build(ir: CodeGenerationIR): CodeGenerationAst {
    val enums = ir.typeDeclarations
        .filter { typeUsed -> typeUsed.kind == TypeDeclaration.KIND_ENUM }
        .map { type -> type.buildEnumType() }

    val customTypes = customTypeMap
        .mapValues { (schemaType, mappedType) ->
          CodeGenerationAst.CustomType(
              name = schemaType.escapeKotlinReservedWord().toUpperCase(),
              schemaType = schemaType,
              mappedType = mappedType
          )
        }

    val inputTypes = ir.typeDeclarations
        .filter { typeUsed -> typeUsed.kind == TypeDeclaration.KIND_INPUT_OBJECT_TYPE }
        .map { type ->
          type.buildInputType(
              schema = schema,
              typesPackageName = typesPackageName,
              customTypes = customTypes
          )
        }

    val fragmentTypes = ir.fragments
        .map { fragment ->
          fragment.buildFragmentType(
              irFragments = ir.fragments,
              customTypes = customTypes
          )
        }.let { fragmentTypes ->
          fragmentTypes.map { fragmentType ->
            fragmentType.copy(
                nestedTypes = fragmentType.nestedTypes.patchTypeHierarchy(fragmentTypes.minus(fragmentType))
            )
          }
        }

    val operations = ir.operations
        .map { operation ->
          operation.buildOperationType(
              irFragments = ir.fragments,
              fragmentTypes = fragmentTypes,
              customTypes = customTypes
          )
        }

    return CodeGenerationAst(
        operationTypes = operations,
        fragmentTypes = fragmentTypes,
        inputTypes = inputTypes,
        enumTypes = enums,
        customTypes = customTypes
    )
  }

  private fun TypeDeclaration.buildEnumType() = CodeGenerationAst.EnumType(
      graphqlName = name,
      name = name.capitalize().escapeKotlinReservedWord(),
      description = description,
      consts = values.map { value ->
        CodeGenerationAst.EnumConst(
            constName = value.name.toUpperCase().escapeKotlinReservedWord(),
            value = value.name,
            description = value.description,
            isDeprecated = value.isDeprecated,
            deprecationReason = value.deprecationReason
        )
      }
  )

  private fun TypeDeclaration.buildInputType(
      schema: IntrospectionSchema,
      typesPackageName: String,
      customTypes: CustomTypes
  ): CodeGenerationAst.InputType {
    return CodeGenerationAst.InputType(
        graphqlName = name,
        name = name.capitalize().escapeKotlinReservedWord(),
        description = description,
        deprecated = false,
        deprecationReason = "",
        fields = fields.map { field ->
          val fieldType = resolveInputFieldType(
              schemaTypeRef = schema.resolveType(schema.resolveType(name)).resolveInputField(field.name).type,
              typesPackageName = typesPackageName,
              customTypes = customTypes
          )
          CodeGenerationAst.InputField(
              name = field.name.decapitalize().escapeKotlinReservedWord(),
              schemaName = field.name,
              deprecated = false,
              deprecationReason = "",
              type = fieldType,
              description = field.description,
              defaultValue = if (fieldType.isCustomScalarField()) null else field.defaultValue
          )
        }
    )
  }

  private fun CodeGenerationAst.FieldType.isCustomScalarField(): Boolean {
    return when (this) {
      is CodeGenerationAst.FieldType.Scalar.Custom -> true
      is CodeGenerationAst.FieldType.Array -> rawType.isCustomScalarField()
      else -> false
    }
  }

  private fun Operation.normalizedOperationName(useSemanticNaming: Boolean, operationNameSuffix: String): String {
    return if (useSemanticNaming && !operationName.endsWith(operationNameSuffix)) {
      operationName.capitalize() + operationNameSuffix
    } else {
      operationName.capitalize()
    }
  }

  private fun Operation.normalizedOperationName(useSemanticNaming: Boolean): String = when (operationType) {
    TYPE_MUTATION -> normalizedOperationName(useSemanticNaming, "Mutation")
    TYPE_QUERY -> normalizedOperationName(useSemanticNaming, "Query")
    TYPE_SUBSCRIPTION -> normalizedOperationName(useSemanticNaming, "Subscription")
    else -> throw IllegalArgumentException("Unknown operation type $operationType")
  }

  private fun Operation.buildOperationType(
      irFragments: List<IrFragment>,
      fragmentTypes: List<CodeGenerationAst.FragmentType>,
      customTypes: CustomTypes
  ): CodeGenerationAst.OperationType {
    val operationType = when {
      isQuery() -> CodeGenerationAst.OperationType.Type.QUERY
      isMutation() -> CodeGenerationAst.OperationType.Type.MUTATION
      isSubscription() -> CodeGenerationAst.OperationType.Type.SUBSCRIPTION
      else -> throw IllegalArgumentException("Unsupported GraphQL operation type: $operationType")
    }
    val operationId = operationOutput.findOperationId(operationName, packageName)
    val operationClassName = normalizedOperationName(useSemanticNaming).capitalize()
    val operationDataType = buildOperationDataType(
        operationClassName = operationClassName,
        irFragments = irFragments,
        fragmentTypes = fragmentTypes,
        customTypes = customTypes
    )
    return CodeGenerationAst.OperationType(
        name = operationClassName,
        packageName = packageName,
        type = operationType,
        operationName = operationName,
        description = description,
        operationId = operationId,
        queryDocument = sourceWithFragments,
        variables = variables.map { variable ->
          val fieldType = resolveInputFieldType(
              schemaTypeRef = variable.type.toIntrospectionTypeRef(schema),
              typesPackageName = typesPackageName,
              customTypes = customTypes
          )
          CodeGenerationAst.InputField(
              name = variable.name.decapitalize().escapeKotlinReservedWord(),
              schemaName = variable.name,
              deprecated = false,
              deprecationReason = "",
              type = fieldType,
              description = "",
              defaultValue = null
          )
        },
        dataType = operationDataType,
        filePath = filePath
    )
  }

  private fun IntrospectionSchema.Type.resolveInputField(name: String): IntrospectionSchema.InputField {
    return (this as? IntrospectionSchema.Type.InputObject)?.inputFields?.find { field -> field.name == name }
        ?: throw IllegalArgumentException("Failed to resolve input field `$name` on type `${this.name}`")
  }

  private fun String.toIntrospectionTypeRef(schema: IntrospectionSchema): IntrospectionSchema.TypeRef {
    return when {
      endsWith("!") -> IntrospectionSchema.TypeRef(
          kind = IntrospectionSchema.Kind.NON_NULL,
          ofType = removeSuffix("!").toIntrospectionTypeRef(schema)
      )

      startsWith("[") && endsWith("]") -> IntrospectionSchema.TypeRef(
          kind = IntrospectionSchema.Kind.LIST,
          ofType = removePrefix("[").removeSuffix("]").toIntrospectionTypeRef(schema)
      )

      else -> schema.resolveType(this)
    }
  }

  private fun resolveInputFieldType(
      schemaTypeRef: IntrospectionSchema.TypeRef,
      typesPackageName: String,
      customTypes: CustomTypes
  ): CodeGenerationAst.FieldType {
    return when (schemaTypeRef.kind) {
      IntrospectionSchema.Kind.ENUM -> CodeGenerationAst.FieldType.Scalar.Enum(
          nullable = true,
          typeRef = CodeGenerationAst.TypeRef(
              name = schemaTypeRef.name!!.capitalize().escapeKotlinReservedWord(),
              packageName = typesPackageName
          )
      )

      IntrospectionSchema.Kind.SCALAR -> {
        when (schemaTypeRef.name!!.toUpperCase()) {
          "STRING" -> CodeGenerationAst.FieldType.Scalar.String(nullable = true)
          "INT" -> CodeGenerationAst.FieldType.Scalar.Int(nullable = true)
          "BOOLEAN" -> CodeGenerationAst.FieldType.Scalar.Boolean(nullable = true)
          "FLOAT" -> CodeGenerationAst.FieldType.Scalar.Float(nullable = true)
          else -> {
            val customType = checkNotNull(customTypes[schemaTypeRef.name])
            CodeGenerationAst.FieldType.Scalar.Custom(
                nullable = true,
                schemaType = schemaTypeRef.name,
                type = customType.mappedType,
                customEnumType = CodeGenerationAst.TypeRef(
                    name = customType.name,
                    packageName = typesPackageName,
                    enclosingType = CodeGenerationAst.customTypeRef(typesPackageName)
                )
            )
          }
        }
      }

      IntrospectionSchema.Kind.NON_NULL -> resolveInputFieldType(
          schemaTypeRef = schemaTypeRef.ofType!!,
          typesPackageName = typesPackageName,
          customTypes = customTypes
      ).nonNullable()

      IntrospectionSchema.Kind.LIST -> CodeGenerationAst.FieldType.Array(
          nullable = true,
          rawType = resolveInputFieldType(
              schemaTypeRef = schemaTypeRef.ofType!!,
              typesPackageName = typesPackageName,
              customTypes = customTypes
          )
      )

      IntrospectionSchema.Kind.INPUT_OBJECT -> {
        CodeGenerationAst.FieldType.Object(
            nullable = true,
            typeRef = CodeGenerationAst.TypeRef(
                name = schemaTypeRef.name!!.capitalize().escapeKotlinReservedWord(),
                packageName = typesPackageName
            )
        )
      }

      else -> throw IllegalArgumentException(
          "Unsupported input field type `$schemaTypeRef`"
      )
    }
  }

  private fun Operation.buildOperationDataType(
      operationClassName: String,
      irFragments: List<IrFragment>,
      fragmentTypes: List<CodeGenerationAst.FragmentType>,
      customTypes: CustomTypes
  ): CodeGenerationAst.OperationDataType {
    val operationSchemaType = when {
      isQuery() -> schema.queryType

      isMutation() -> schema.mutationType

      isSubscription() -> schema.subscriptionType

      else -> throw IllegalArgumentException("Unsupported GraphQL operation type: `$operationType`")
    }
    val nestedTypeContainer = ObjectTypeContainerBuilder(
        packageName = "",
        reservedTypeNames = setOf(operationClassName)
    )
    val objectTypeBuilder = ObjectTypeBuilder(
        schema = schema,
        customTypes = customTypes,
        typesPackageName = typesPackageName,
        fragmentsPackage = fragmentsPackage,
        irFragments = irFragments.associateBy { it.fragmentName },
        nestedTypeContainer = nestedTypeContainer,
        enclosingType = CodeGenerationAst.TypeRef(name = operationClassName)
    )
    val dataFieldType = objectTypeBuilder.resolveFieldType(
        field = Field(
            responseName = "data",
            fieldName = "data",
            type = operationSchemaType,
            typeDescription = "",
            sourceLocation = SourceLocation.UNKNOWN,
            fields = fields,
            fragmentRefs = fragments,
            inlineFragments = inlineFragments
        ),
        schemaTypeRef = schema.resolveType(operationSchemaType),
        abstract = false
    ) as CodeGenerationAst.FieldType.Object
    return CodeGenerationAst.OperationDataType(
        rootType = dataFieldType.typeRef,
        nestedTypes = nestedTypeContainer.typeContainer.patchTypeHierarchy(fragmentTypes)
    )
  }

  private fun IrFragment.buildFragmentType(
      irFragments: List<IrFragment>,
      customTypes: CustomTypes
  ): CodeGenerationAst.FragmentType {
    val nestedTypeContainer = ObjectTypeContainerBuilder(packageName = fragmentsPackage)
    val rootType = nestedTypeContainer.registerObjectType(typeName = fragmentName, enclosingType = null) { typeRef ->
      val objectTypeBuilder = ObjectTypeBuilder(
          schema = schema,
          customTypes = customTypes,
          typesPackageName = typesPackageName,
          fragmentsPackage = fragmentsPackage,
          irFragments = irFragments.associateBy { it.fragmentName },
          nestedTypeContainer = nestedTypeContainer,
          enclosingType = typeRef
      )
      objectTypeBuilder.buildObjectType(
          typeRef = typeRef,
          schemaType = schema.resolveType(schema.resolveType(typeCondition)),
          fields = fields,
          abstract = true
      )
    }

    val objectTypeBuilder = ObjectTypeBuilder(
        schema = schema,
        customTypes = customTypes,
        typesPackageName = typesPackageName,
        fragmentsPackage = fragmentsPackage,
        irFragments = irFragments.associateBy { it.fragmentName },
        nestedTypeContainer = nestedTypeContainer,
        enclosingType = rootType
    )
    val defaultImplementationType = if (inlineFragments.isEmpty() && fragmentRefs.isEmpty()) {
      nestedTypeContainer.registerObjectType(
          typeName = "DefaultImpl",
          enclosingType = rootType
      ) { typeRef ->
        objectTypeBuilder.buildObjectType(
            typeRef = typeRef,
            schemaType = schema.resolveType(schema.resolveType(typeCondition)),
            fields = fields,
            abstract = false,
            implements = setOf(rootType)
        )
      }
    } else {
      objectTypeBuilder.buildObjectTypeWithFragments(
          schemaType = schema.resolveType(schema.resolveType(typeCondition)),
          typeName = "DefaultImpl",
          fields = fields,
          inlineFragments = inlineFragments,
          fragmentRefs = fragmentRefs,
          singularizeName = false,
          abstract = false,
          implements = setOf(rootType)
      )
    }
    return CodeGenerationAst.FragmentType(
        graphqlName = fragmentName,
        rootType = rootType,
        defaultImplementation = defaultImplementationType,
        nestedTypes = nestedTypeContainer.typeContainer,
        fragmentDefinition = source
    )
  }
}
