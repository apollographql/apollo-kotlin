package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.selections.CompiledSelectionsBuilder
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.javapoet.ClassName

internal class OperationSelectionsBuilder(
    val context: JavaContext,
    val operation: IrOperation,
) : JavaClassBuilder {
  private val packageName = context.layout.operationResponseFieldsPackageName(operation.filePath)
  private val simpleName = context.layout.operationSelectionsName(operation)

  override fun prepare() {
    context.resolver.registerOperationSelections(
        operation.name,
        ClassName.get(packageName, simpleName)
    )
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = CompiledSelectionsBuilder(
            context = context,
        ).build(
            selectionSets = operation.selectionSets,
            rootName = simpleName,
        )
    )
  }
}
