package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.compiler.ast.CodeGenerationAst
import com.squareup.kotlinpoet.TypeSpec

internal fun CodeGenerationAst.OperationDataType.toOperationDataTypeSpec(
    targetPackage: String,
    operationName: String,
): TypeSpec {
  val dataType = checkNotNull(nestedTypes[rootType]) {
    "Failed to resolve operation root data type"
  }
  val operationResponseAdapter = CodeGenerationAst.TypeRef(
      name = operationName,
      packageName = targetPackage
  ).asAdapterTypeName()
  return dataType
      .copy(
          description = "Data from the response after executing this GraphQL operation",
          implements = dataType.implements + CodeGenerationAst.TypeRef(
              name = "Data",
              enclosingType = CodeGenerationAst.TypeRef(
                name = Operation::class.java.simpleName,
                packageName = Operation::class.java.`package`.name
              )
          )
      )
      .typeSpec(responseAdapter = operationResponseAdapter)
}
