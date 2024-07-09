package com.apollographql.apollo.compiler.codegen.java.operations

import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo.compiler.codegen.java.operations.util.ResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.maybeFlatten
import com.apollographql.apollo.compiler.codegen.operationAdapterPackageName
import com.apollographql.apollo.compiler.codegen.operationName
import com.apollographql.apollo.compiler.codegen.responseAdapter
import com.apollographql.apollo.compiler.ir.IrOperation
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class OperationResponseAdapterBuilder(
    val context: JavaOperationsContext,
    val operation: IrOperation,
    val flatten: Boolean,
) : JavaClassBuilder {
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

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addTypes(
            responseAdapterBuilders.flatMap { it.build() }
        )
        .build()
  }
}
