package com.apollographql.apollo.compiler.codegen.kotlin.operations

import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOperationsContext
import com.apollographql.apollo.compiler.codegen.kotlin.operations.util.ResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.maybeFlatten
import com.apollographql.apollo.compiler.codegen.operationAdapterPackageName
import com.apollographql.apollo.compiler.codegen.operationName
import com.apollographql.apollo.compiler.codegen.responseAdapter
import com.apollographql.apollo.compiler.ir.IrOperation
import com.squareup.kotlinpoet.TypeSpec

internal class OperationResponseAdapterBuilder(
    val context: KotlinOperationsContext,
    val operation: IrOperation,
    val flatten: Boolean,
) : CgFileBuilder {
  private val packageName = context.layout.operationAdapterPackageName(operation.normalizedFilePath)
  private val simpleName = context.layout.operationName(operation).responseAdapter()

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