package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationOutput


interface OperationOutputGenerator {
  fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput

  val version: String

  class DefaultOperationOuputGenerator(val operationIdGenerator: OperationIdGenerator) : OperationOutputGenerator {
    override fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput {
      return operationDescriptorList.map {
        operationIdGenerator.apply(it.source, it.filePath) to it
      }.toMap()
    }

    override val version = operationIdGenerator.version
  }
}