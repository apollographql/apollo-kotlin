package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.selections.CompiledSelectionsBuilder
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.kotlinpoet.ClassName

internal class OperationSelectionsBuilder(
    val context: KotlinContext,
    val operation: IrOperation,
) : CgFileBuilder {
  private val packageName = context.layout.operationResponseFieldsPackageName(operation.filePath)
  private val simpleName = context.layout.operationSelectionsName(operation)

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
