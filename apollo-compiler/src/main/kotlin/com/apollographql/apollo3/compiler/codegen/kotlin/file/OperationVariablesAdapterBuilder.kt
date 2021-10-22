package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

class OperationVariablesAdapterBuilder(
    val context: KotlinContext,
    val operation: IrOperation
): CgOutputFileBuilder {
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