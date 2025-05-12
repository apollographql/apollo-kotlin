package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationId

internal class LegacyOperationIdsGenerator(private val plugin: ApolloCompilerPlugin): OperationIdsGenerator {
  override fun generate(operationDescriptorList: Collection<OperationDescriptor>): List<OperationId> {
    @Suppress("DEPRECATION")
    val operationIds = plugin.operationIds(operationDescriptorList.toList())
    if (operationIds != null) {
      println("Apollo: using ApolloCompiler.operationIds() is deprecated. Please use registry.registerOperationIdsGenerator() instead.")
      return operationIds
    }
    return NoList
  }

  companion object {
    internal val NoList: List<OperationId> = emptyList()
  }
}
