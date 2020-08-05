package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.CustomTypes
import com.apollographql.apollo.compiler.ast.EnumType
import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.Schema
import com.apollographql.apollo.compiler.ast.TypeRef
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.apollographql.apollo.compiler.operationoutput.OperationOutput

internal fun CodeGenerationIR.ast(
    customTypeMap: CustomTypes,
    typesPackageName: String,
    fragmentsPackage: String,
    useSemanticNaming: Boolean,
    operationOutput: OperationOutput
): Schema {
  val enums = typesUsed.filter { it.kind == TypeDeclaration.KIND_ENUM }.map { it.ast() }
  val inputTypes = typesUsed.filter { it.kind == TypeDeclaration.KIND_INPUT_OBJECT_TYPE }.map {
    it.ast(
        enums = enums,
        customTypeMap = customTypeMap,
        typesPackageName = typesPackageName
    )
  }
  val irFragments = fragments.associateBy { it.fragmentName }
  val fragments = fragments.filter { it.filePath != null }.map {
    it.ast(
        Context(
            customTypeMap = customTypeMap,
            enums = enums,
            typesPackageName = typesPackageName,
            fragmentsPackage = fragmentsPackage,
            fragments = irFragments
        )
    )
  }
  val operations = operations.map { operation ->
    operation.ast(
        operationClassName = operation.normalizedOperationName(useSemanticNaming).capitalize(),
        context = Context(
            customTypeMap = customTypeMap,
            enums = enums,
            typesPackageName = typesPackageName,
            fragmentsPackage = fragmentsPackage,
            fragments = irFragments
        ),
        operationOutput = operationOutput
    )
  }
  return Schema(
      enums = enums,
      customTypes = customTypeMap,
      inputTypes = inputTypes,
      fragments = fragments,
      operations = operations
  )
}

internal fun resolveFieldType(
    graphQLType: String,
    enums: List<EnumType>,
    customTypeMap: Map<String, String>,
    typesPackageName: String
): FieldType {
  val isGraphQLArrayType = graphQLType.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') }
  if (isGraphQLArrayType) {
    return FieldType.Array(
        rawType = resolveFieldType(
            graphQLType = graphQLType.removeSuffix("!").removePrefix("[").removeSuffix("]").removeSuffix("!"),
            enums = enums,
            customTypeMap = customTypeMap,
            typesPackageName = typesPackageName
        ),
        isOptional = !graphQLType.removeSuffix("!").removeSuffix("]").endsWith("!")
    )
  } else {
    return when (ScalarType.forName(graphQLType.removeSuffix("!"))) {
      is ScalarType.STRING -> FieldType.Scalar.String
      is ScalarType.INT -> FieldType.Scalar.Int
      is ScalarType.BOOLEAN -> FieldType.Scalar.Boolean
      is ScalarType.FLOAT -> FieldType.Scalar.Float
      else -> {
        val normalizedName = graphQLType.removeSuffix("!").capitalize().escapeKotlinReservedWord()
        when {
          enums.find { it.name == normalizedName } != null -> FieldType.Scalar.Enum(
              TypeRef(
                  name =  normalizedName,
                  packageName = typesPackageName
              )
          )
          customTypeMap.containsKey(graphQLType.removeSuffix("!")) -> FieldType.Scalar.Custom(
              schemaType = graphQLType.removeSuffix("!"),
              mappedType = customTypeMap.getValue(graphQLType.removeSuffix("!")),
              customEnumConst = normalizedName.toUpperCase(),
              customEnumType = TypeRef(
                  name = "CustomType",
                  packageName = typesPackageName
              )
          )
          else -> FieldType.Object(
              TypeRef(
                  name = normalizedName,
                  packageName = typesPackageName
              )
          )
        }
      }
    }
  }
}
