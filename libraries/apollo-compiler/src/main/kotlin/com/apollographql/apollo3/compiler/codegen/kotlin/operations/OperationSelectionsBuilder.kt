package com.apollographql.apollo.compiler.codegen.kotlin.operations

import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOperationsContext
import com.apollographql.apollo.compiler.codegen.operationName
import com.apollographql.apollo.compiler.codegen.operationResponseFieldsPackageName
import com.apollographql.apollo.compiler.codegen.selections
import com.apollographql.apollo.compiler.ir.IrOperation
import com.squareup.kotlinpoet.ClassName

internal class OperationSelectionsBuilder(
    val context: KotlinOperationsContext,
    val operation: IrOperation,
) : CgFileBuilder {
  private val packageName = context.layout.operationResponseFieldsPackageName(operation.normalizedFilePath)
  private val simpleName = context.layout.operationName(operation).selections()

  override fun prepare() {
    context.resolver.registerOperationSelections(
        operation.name,
        ClassName(packageName, simpleName)
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(
            CompiledSelectionsBuilder(
                context = context,
            ).build(
                selectionSets = operation.selectionSets,
                rootName = simpleName,
            )
        )
    )
  }
}