package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.adapter.ResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.kotlinpoet.TypeSpec

class OperationResponseAdapterBuilder(
    val context: CgContext,
    val operation: IrOperation,
) : CgFileBuilder {
  private val packageName = context.layout.operationAdapterPackageName(operation.filePath)
  private val simpleName = context.layout.operationResponseAdapterWrapperName(operation)

  private val responseAdapterBuilders = operation.dataModelGroup.maybeFlatten(false).map {
    ResponseAdapterBuilder.create(
        context = context,
        modelGroup = it,
        path = listOf(packageName, simpleName)
    )
  }

  override fun prepare() {
    responseAdapterBuilders.forEach { it.prepare() }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(typeSpec())
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(simpleName)
        .addTypes(
            responseAdapterBuilders.flatMap { it.build() }
        )
        .build()
  }
}