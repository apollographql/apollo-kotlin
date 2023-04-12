package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.adapter.variableAdapterTypeSpec
import com.apollographql.apollo3.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec

internal class OperationVariablesAdapterBuilder(
    val context: JavaContext,
    val operation: IrOperation,
) : JavaClassBuilder {
  val packageName = context.layout.operationAdapterPackageName(operation.filePath)
  val simpleName = context.layout.operationVariablesAdapterName(operation)
  override fun prepare() {
    context.resolver.registerOperationVariablesAdapter(
        operation.name,
        ClassName.get(packageName, simpleName)
    )
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  private fun typeSpec(): TypeSpec {
    return operation.variables.map { it.toNamedType() }
        .variableAdapterTypeSpec(
            context = context,
            adapterName = simpleName,
            adaptedTypeName = context.resolver.resolveOperation(operation.name),
        )
  }
}
