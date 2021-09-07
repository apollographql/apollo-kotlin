package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.selections.CompiledSelectionsBuilder
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.javapoet.ClassName

class OperationSelectionsBuilder(
    val context: JavaContext,
    val operation: IrOperation,
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
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
            allFragmentDefinitions = allFragmentDefinitions,
            schema = schema
        ).build(
            selections = operation.selections,
            rootName = simpleName,
            parentType = operation.typeCondition
        )
    )
  }
}