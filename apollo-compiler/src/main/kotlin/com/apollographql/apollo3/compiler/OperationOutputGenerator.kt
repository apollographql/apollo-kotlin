package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput


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