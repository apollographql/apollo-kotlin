package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.operationoutput.OperationList
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.gradle.api.ApolloGenerateOperationIdsTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

abstract class ApolloGenerateDefaultOperationIdsTask : ApolloGenerateOperationIdsTask() {
  @get: Internal
  lateinit var operationIdGenerator: OperationIdGenerator

  @Input
  fun getOperationIdGeneratorVersion() = operationIdGenerator.version

  override fun generateOperationOutput(operationList: OperationList): OperationOutput {
    return operationList.map {
      operationIdGenerator.apply(it.source, it.filePath) to it
    }.toMap()
  }
}


