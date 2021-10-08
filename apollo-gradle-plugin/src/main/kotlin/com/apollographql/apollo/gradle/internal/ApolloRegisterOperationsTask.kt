package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class ApolloRegisterOperationsTask: DefaultTask() {
  @get:InputFile
  abstract val operationOutput: RegularFileProperty

  @get:Input
  abstract val key: Property<String>

  @get:Input
  abstract val graph: Property<String>

  @get:Input
  abstract val graphVariant: Property<String>

  @TaskAction
  fun taskAction() {
    RegisterOperations.registerOperations(
        key = key.get() ?: error("key is required to register operations"),
        graphID = graph.get() ?: error("graphID is required to register operations"),
        graphVariant = graphVariant.get() ?: error("graphVariant is required to register operations"),
        operationOutput = OperationOutput(operationOutput.get().asFile)
    )
  }
}