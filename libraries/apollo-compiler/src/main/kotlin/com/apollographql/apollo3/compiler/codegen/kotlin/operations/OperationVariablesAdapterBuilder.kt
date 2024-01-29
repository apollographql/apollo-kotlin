package com.apollographql.apollo3.compiler.codegen.kotlin.operations

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.variablesAdapterTypeSpec
import com.apollographql.apollo3.compiler.codegen.operationAdapterPackageName
import com.apollographql.apollo3.compiler.codegen.operationName
import com.apollographql.apollo3.compiler.codegen.variablesAdapter
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal class OperationVariablesAdapterBuilder(
    val context: KotlinContext,
    val operation: IrOperation,
) : CgFileBuilder {
  val packageName = context.layout.operationAdapterPackageName(operation.normalizedFilePath)
  val simpleName = context.layout.operationName(operation).variablesAdapter()
  override fun prepare() {
    context.resolver.registerOperationVariablesAdapter(
        operation.name,
        ClassName(packageName, simpleName)
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(typeSpec())
    )
  }

  private fun typeSpec(): TypeSpec {
    return operation.variables
        .variablesAdapterTypeSpec(
            context = context,
            adapterName = simpleName,
            adaptedTypeName = context.resolver.resolveOperation(operation.name),
        )
  }
}