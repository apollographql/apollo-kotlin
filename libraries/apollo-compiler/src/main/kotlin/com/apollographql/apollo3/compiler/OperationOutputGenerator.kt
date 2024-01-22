package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
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
   * When using the compiler outside the Apollo Gradle Plugin context, [version] is not accessed.
   */
  @Deprecated("Load PackageNameGenerator through plugins")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val version: String
    get() {
      error("this should only be called from the Apollo Gradle Plugin")
    }


  object Default : OperationOutputGenerator {
    override fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput {
      return operationDescriptorList.associateBy {
        OperationIdGenerator.Sha256.apply(it.source, it.name)
      }
    }
  }
}