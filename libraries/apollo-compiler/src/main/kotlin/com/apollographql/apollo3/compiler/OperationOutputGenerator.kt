package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput

/**
 * A class that generates [OperationOutput] to compute the operationIds. [OperationOutputGenerator] works with a collection of
 * [OperationDescriptor] for implementation that need to batch compute the operationIds. For an example to send all the operation
 * at once to a backend for whitelisting.
 *
 * If you don't need batch compute, use [OperationOutputGenerator.Default]
 */
interface OperationOutputGenerator {
  /**
   * Generate
   *
   * If used in a Gradle context, the [generate] function must be pure else up-to-date checks will fail
   */
  fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput

  /**
   * A version that is used as input of the Gradle task. Since [OperationOutputGenerator] cannot easily be serialized and influences
   * the output of the task, we need a way to mark the task out-of-date when the implementation changes.
   *
   * Two different implementations **must** have different versions.
   *
   * When using the compiler outside of a Gradle context, [version] is not used, making it the empty string is fine.
   */
  val version: String

  class Default(private val operationIdGenerator: OperationIdGenerator) : OperationOutputGenerator {
    override fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput {
      return operationDescriptorList.map {
        operationIdGenerator.apply(it.source, it.name) to it
      }.toMap()
    }

    override val version = operationIdGenerator.version
  }
}