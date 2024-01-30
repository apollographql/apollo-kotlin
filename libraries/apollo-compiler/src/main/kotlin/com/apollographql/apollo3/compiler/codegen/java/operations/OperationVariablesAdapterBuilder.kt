package com.apollographql.apollo3.compiler.codegen.java.operations

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.adapter.variableAdapterTypeSpec
import com.apollographql.apollo3.compiler.codegen.operationAdapterPackageName
import com.apollographql.apollo3.compiler.codegen.operationName
import com.apollographql.apollo3.compiler.codegen.variablesAdapter
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec

internal class OperationVariablesAdapterBuilder(
    val context: JavaContext,
    val operation: IrOperation,
) : JavaClassBuilder {
  val packageName = context.layout.operationAdapterPackageName(operation.normalizedFilePath)
  val simpleName = context.layout.operationName(operation).variablesAdapter()
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
    return operation.variables
        .variableAdapterTypeSpec(
            context = context,
            adapterName = simpleName,
            adaptedTypeName = context.resolver.resolveOperation(operation.name),
        )
  }
}
