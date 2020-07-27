package com.apollographql.apollo.compiler.next.ast

import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.ast.CustomTypes
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.Operation
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.resolveType
import com.apollographql.apollo.compiler.ir.Fragment as IrFragment

internal fun CodeGenerationIR.buildCodeGenerationAst(
    schema: IntrospectionSchema,
    customTypeMap: CustomTypes,
    typesPackageName: String,
    fragmentsPackage: String,
    useSemanticNaming: Boolean,
    operationIdGenerator: OperationIdGenerator
): CodeGenerationAst {
  return CodeGenerationAstBuilder(
      schema = schema,
      customTypeMap = customTypeMap,
      typesPackageName = typesPackageName,
      fragmentsPackage = fragmentsPackage,
      useSemanticNaming = useSemanticNaming,
      operationIdGenerator = operationIdGenerator
  ).build(this)
}

private class CodeGenerationAstBuilder(
    private val schema: IntrospectionSchema,
    private val customTypeMap: CustomTypes,
    private val typesPackageName: String,
    private val fragmentsPackage: String,
    private val useSemanticNaming: Boolean,
    private val operationIdGenerator: OperationIdGenerator
) {

  fun build(ir: CodeGenerationIR): CodeGenerationAst {
    val enums = ir.typesUsed
        .filter { typeUsed -> typeUsed.kind == TypeDeclaration.KIND_ENUM }
        .map { type -> type.buildEnumType() }

    val inputTypes = ir.typesUsed
        .filter { typeUsed -> typeUsed.kind == TypeDeclaration.KIND_INPUT_OBJECT_TYPE }
        .map { type ->
          type.buildInputType(
              schema = schema,
              typesPackageName = typesPackageName,
              customTypeMap = customTypeMap
          )
        }

    val fragmentTypes = ir.fragments.map { fragment ->
      fragment.buildFragmentType(ir.fragments)
    }

    val operations = ir.operations.map { operation ->
      operation.buildOperationType(
          irFragments = ir.fragments,
          fragmentTypes = fragmentTypes
      )
    }

    return CodeGenerationAst(
        operationTypes = operations,
        fragmentTypes = fragmentTypes,
        inputTypes = inputTypes,
        enumTypes = enums
    )
  }

  private fun TypeDeclaration.buildEnumType() = CodeGenerationAst.EnumType(
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
      customTypeMap: CustomTypes
  ): CodeGenerationAst.InputType {
    return CodeGenerationAst.InputType(
        name = name.capitalize().escapeKotlinReservedWord(),
        description = description,
        deprecated = false,
        deprecationReason = "",
        fields = fields.map { field ->
          val fieldType = resolveInputFieldType(
              schemaTypeRef = schema.resolveType(schema.resolveType(name)).resolveInputField(field.name).type,
              typesPackageName = typesPackageName,
              customTypeMap = customTypeMap
          )
          CodeGenerationAst.InputField(
              name = field.name.decapitalize().escapeKotlinReservedWord(),
              schemaName = field.name,
              deprecated = false,
              deprecationReason = "",
              type = fieldType,
              description = field.description,
              defaultValue = if (fieldType is CodeGenerationAst.FieldType.Scalar.Custom) null else field.defaultValue
          )
        }
    )
  }

  private fun Operation.buildOperationType(
      irFragments: List<IrFragment>,
      fragmentTypes: List<CodeGenerationAst.FragmentType>
  ): CodeGenerationAst.OperationType {
    val operationType = when {
      isQuery() -> CodeGenerationAst.OperationType.Type.QUERY
      isMutation() -> CodeGenerationAst.OperationType.Type.MUTATION
      isSubscription() -> CodeGenerationAst.OperationType.Type.SUBSCRIPTION
      else -> throw IllegalArgumentException("Unsupported GraphQL operation type: $operationType")
    }
    val operationDataType = buildOperationDataType(
        irFragments = irFragments,
        fragmentTypes = fragmentTypes
    )
    val operationId = operationIdGenerator.apply(QueryDocumentMinifier.minify(sourceWithFragments), filePath)
    val operationClassName = normalizedOperationName(useSemanticNaming).capitalize()
    return CodeGenerationAst.OperationType(
        name = operationClassName,
        type = operationType,
        operationName = operationName,
        description = description,
        operationId = operationId,
        queryDocument = sourceWithFragments,
        variables = variables.map { variable ->
          val fieldType = resolveInputFieldType(
              schemaTypeRef = variable.type.toIntrospectionTypeRef(schema),
              typesPackageName = typesPackageName,
              customTypeMap = customTypeMap
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
          ofType = removeSuffix("!").toIntrospectionTypeRef(schema)
      )

      else -> schema.resolveType(this)
    }
  }

  private fun resolveInputFieldType(
      schemaTypeRef: IntrospectionSchema.TypeRef,
      typesPackageName: String,
      customTypeMap: CustomTypes
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
          else -> CodeGenerationAst.FieldType.Scalar.Custom(
              nullable = true,
              schemaType = schemaTypeRef.name,
              type = customTypeMap[schemaTypeRef.name]!!
          )
        }
      }

      IntrospectionSchema.Kind.NON_NULL -> resolveInputFieldType(
          schemaTypeRef = schemaTypeRef.ofType!!,
          typesPackageName = typesPackageName,
          customTypeMap = customTypeMap
      ).nonNullable()

      IntrospectionSchema.Kind.LIST -> CodeGenerationAst.FieldType.Array(
          nullable = true,
          rawType = resolveInputFieldType(
              schemaTypeRef = schemaTypeRef.ofType!!,
              typesPackageName = typesPackageName,
              customTypeMap = customTypeMap
          )
      )

      else -> throw IllegalArgumentException(
          "Unsupported input field type `$schemaTypeRef`"
      )
    }
  }

  private fun Operation.buildOperationDataType(
      irFragments: List<IrFragment>,
      fragmentTypes: List<CodeGenerationAst.FragmentType>
  ): CodeGenerationAst.OperationDataType {
    val operationSchemaType = when {
      isQuery() -> schema.resolveType(schema.resolveType(schema.queryType))

      isMutation() -> schema.resolveType(schema.resolveType(schema.mutationType))

      isSubscription() -> schema.resolveType(schema.resolveType(schema.subscriptionType))

      else -> throw IllegalArgumentException("Unsupported GraphQL operation type: `$operationType`")
    }
    val nestedTypeContainer = ObjectTypeContainerBuilder(packageName = "")

    val rootType = nestedTypeContainer.registerObjectType(typeName = "Data", enclosingType = null) { typeRef ->
      buildObjectType(
          typeRef = typeRef,
          schemaType = operationSchemaType,
          fields = fields,
          schema = schema,
          customTypeMap = customTypeMap,
          typesPackageName = typesPackageName,
          fragmentsPackage = fragmentsPackage,
          irFragments = irFragments,
          nestedTypeContainer = nestedTypeContainer
      )
    }
    return CodeGenerationAst.OperationDataType(
        rootType = rootType,
        nestedTypes = nestedTypeContainer.typeContainer.patchTypeHierarchy(rootType, fragmentTypes)
    )
  }

  private fun IrFragment.buildFragmentType(irFragments: List<IrFragment>): CodeGenerationAst.FragmentType {
    val nestedTypeContainer = ObjectTypeContainerBuilder(packageName = fragmentsPackage)
    val rootType = nestedTypeContainer.registerObjectType(typeName = fragmentName, enclosingType = null) { typeRef ->
      buildInterfaceType(
          typeRef = typeRef,
          schemaType = schema.resolveType(schema.resolveType(typeCondition)),
          fields = fields,
          schema = schema,
          customTypeMap = customTypeMap,
          typesPackageName = typesPackageName,
          fragmentsPackage = fragmentsPackage,
          irFragments = irFragments,
          nestedTypeContainer = nestedTypeContainer
      )
    }
    return CodeGenerationAst.FragmentType(
        rootType = rootType,
        nestedTypes = nestedTypeContainer.typeContainer
    )
  }
}
