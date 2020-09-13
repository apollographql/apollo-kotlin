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
      implements = dataType.implements + CodeGenerationAst.TypeRef(
          name = "Data",
          packageName = Operation::class.java.`package`.name,
          enclosingType = CodeGenerationAst.TypeRef(name = Operation::class.java.simpleName)
      )
  ).typeSpec()
}
