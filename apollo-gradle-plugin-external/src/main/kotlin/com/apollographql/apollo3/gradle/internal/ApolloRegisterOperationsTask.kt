package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.MOSHI
import com.apollographql.apollo3.compiler.fromJson
import com.apollographql.apollo3.compiler.getJsonAdapter
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.buffer
import okio.source
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
        operationOutput = operationOutputAdapter().fromJson(operationOutput.get().asFile.source().buffer())!!
    )
  }

  private fun operationOutputAdapter(): JsonAdapter<OperationOutput> {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, OperationDescriptor::class.java)
    return MOSHI.adapter<OperationOutput>(type)
  }
}