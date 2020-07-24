package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptorList
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.gradle.api.ApolloGenerateOperationIdsTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

abstract class ApolloGenerateDefaultOperationIdsTask : ApolloGenerateOperationIdsTask() {
  @get: Internal
  lateinit var operationIdGenerator: OperationIdGenerator

  @Input
  fun getOperationIdGeneratorVersion() = operationIdGenerator.version

  override fun generateOperationOutput(operationDescriptorList: OperationDescriptorList): OperationOutput {
    return operationDescriptorList.map {
      operationIdGenerator.apply(it.source, it.filePath) to it
    }.toMap()
  }
}