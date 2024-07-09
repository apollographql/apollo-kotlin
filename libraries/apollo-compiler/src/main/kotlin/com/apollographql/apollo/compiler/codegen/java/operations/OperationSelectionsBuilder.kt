package com.apollographql.apollo.compiler.codegen.java.operations

import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo.compiler.codegen.operationName
import com.apollographql.apollo.compiler.codegen.operationResponseFieldsPackageName
import com.apollographql.apollo.compiler.codegen.selections
import com.apollographql.apollo.compiler.ir.IrOperation
import com.squareup.javapoet.ClassName

internal class OperationSelectionsBuilder(
    val context: JavaOperationsContext,
    val operation: IrOperation,
) : JavaClassBuilder {
  private val packageName = context.layout.operationResponseFieldsPackageName(operation.normalizedFilePath)
  private val simpleName = context.layout.operationName(operation).selections()

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
