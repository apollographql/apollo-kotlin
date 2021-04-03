package com.apollographql.apollo3.compiler.unified.codegen.file

import com.apollographql.apollo3.compiler.unified.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.codegen.CgFile
import com.apollographql.apollo3.compiler.unified.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.unified.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.ir.IrOperation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

class OperationVariablesAdapterBuilder(
    val context: CgContext,
    val operation: IrOperation
): CgFileBuilder {
  val packageName = context.layout.operationAdapterPackageName(operation.filePath)
  val simpleName = context.layout.operationVariablesAdapterName(operation)
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
    return operation.variables.map { it.toNamedType() }
        .inputAdapterTypeSpec(
            context = context,
            adapterName = simpleName,
            adaptedTypeName = context.resolver.resolveOperation(operation.name)
        )
  }
}