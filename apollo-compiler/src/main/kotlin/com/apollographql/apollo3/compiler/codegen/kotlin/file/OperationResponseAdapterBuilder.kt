package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.ResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.kotlinpoet.TypeSpec

class OperationResponseAdapterBuilder(
    val context: KotlinContext,
    val operation: IrOperation,
    val flatten: Boolean,
) : CgOutputFileBuilder {
  private val packageName = context.layout.operationAdapterPackageName(operation.filePath)
  private val simpleName = context.layout.operationResponseAdapterWrapperName(operation)

  private val responseAdapterBuilders = operation.dataModelGroup.maybeFlatten(flatten).map {
    ResponseAdapterBuilder.create(
        context = context,
        modelGroup = it,
        path = listOf(packageName, simpleName),
        true
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