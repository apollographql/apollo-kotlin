package com.apollographql.apollo.compiler.next.codegen

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.compiler.next.ast.CodeGenerationAst
import com.squareup.kotlinpoet.TypeSpec

internal fun CodeGenerationAst.OperationDataType.toOperationDataTypeSpec(): TypeSpec {
  val dataType = checkNotNull(nestedTypes[rootType]) {
    "Failed to resolve operation root data type"
  }
  return dataType.copy(
      description = "Data from the response after executing this GraphQL operation",
      implements = setOf(
          CodeGenerationAst.TypeRef(
              name = "Data",
              enclosingType = CodeGenerationAst.TypeRef(
                  name = Operation::class.java.simpleName,
                  packageName = Operation::class.java.`package`.name
              )
          )
      )
  ).typeSpec()
}
