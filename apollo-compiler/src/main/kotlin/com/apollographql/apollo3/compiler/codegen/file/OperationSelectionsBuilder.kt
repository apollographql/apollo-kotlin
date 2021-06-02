package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.selections.CompiledSelectionsBuilder
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.kotlinpoet.MemberName

class OperationSelectionsBuilder(
    val context: CgContext,
    val operation: IrOperation,
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
) : CgFileBuilder {
  private val packageName = context.layout.operationResponseFieldsPackageName(operation.filePath)
  private val simpleName = context.layout.operationSelectionsName(operation)

  override fun prepare() {
    context.resolver.registerOperationSelections(
        operation.name,
        MemberName(packageName, simpleName)
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        propertySpecs = CompiledSelectionsBuilder(
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