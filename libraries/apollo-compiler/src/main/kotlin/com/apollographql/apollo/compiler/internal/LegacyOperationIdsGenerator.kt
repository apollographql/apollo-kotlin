package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.OperationIdsGenerator
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationId

internal class LegacyOperationIdsGenerator(private val plugin: ApolloCompilerPlugin): OperationIdsGenerator {
  override fun generate(operationDescriptors: List<OperationDescriptor>): List<OperationId> {
    @Suppress("DEPRECATION")
    val operationIds = plugin.operationIds(operationDescriptors)
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