package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.compiler.operationoutput.OperationDescriptorList
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ApolloGenerateOperationIdsTask : DefaultTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val operationDescriptorListFile: RegularFileProperty

  @get:OutputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val operationOutputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val operationDescriptorList = OperationDescriptorList(operationDescriptorListFile.get().asFile)
    operationOutputFile.get().asFile.writeText(generateOperationOutput(operationDescriptorList).toJson())
  }

  abstract fun generateOperationOutput(operationDescriptorList: OperationDescriptorList): OperationOutput
}