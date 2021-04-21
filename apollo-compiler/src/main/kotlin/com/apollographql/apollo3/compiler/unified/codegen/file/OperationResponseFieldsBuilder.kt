package com.apollographql.apollo3.compiler.unified.codegen.file

import com.apollographql.apollo3.compiler.unified.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.codegen.CgFile
import com.apollographql.apollo3.compiler.unified.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.unified.codegen.responsefields.ResponseFieldsBuilder
import com.apollographql.apollo3.compiler.unified.ir.IrOperation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

class OperationResponseFieldsBuilder(
    val context: CgContext,
    val operation: IrOperation,
) : CgFileBuilder {
  private val packageName = context.layout.operationResponseFieldsPackageName(operation.filePath)
  private val simpleName = context.layout.operationResponseFieldsName(operation)

  private val responseFieldsBuilder = ResponseFieldsBuilder(
      rootField = operation.field,
      rootName = simpleName
  )

  override fun prepare() {
    context.resolver.registerOperationResponseFields(
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
    return responseFieldsBuilder.build()
  }
}