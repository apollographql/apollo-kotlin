@file:Suppress("DEPRECATION")

package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.LayoutFactory
import com.apollographql.apollo.compiler.OperationIdsGenerator
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationId

internal class LegacyOperationIdsGenerator(private val plugin: ApolloCompilerPlugin) : OperationIdsGenerator {
  override fun generate(operationDescriptors: List<OperationDescriptor>): List<OperationId> {
    @Suppress("DEPRECATION")
    val operationIds = plugin.operationIds(operationDescriptors.toList())
    if (operationIds != null) {
      println("Apollo: using ApolloCompiler.operationIds() is deprecated. Please use registry.registerOperationIdsGenerator() from beforeCompilationStep() instead.")
      return operationIds
    }
    return NoList
  }

  companion object {
    internal val NoList: List<OperationId> = emptyList()
  }
}


internal class LegacyLayoutFactory(private val plugin: ApolloCompilerPlugin) : LayoutFactory {
  override fun create(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
    val layout = plugin.layout(codegenSchema)
    if (layout != null) {
      error("Apollo: using ApolloCompilerPlugin.layout() is deprecated. Please use registry.registerLayout() from beforeCompilationStep() instead.")
    }
    return layout
  }
}
