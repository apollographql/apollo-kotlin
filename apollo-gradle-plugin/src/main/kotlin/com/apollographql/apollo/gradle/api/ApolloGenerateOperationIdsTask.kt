package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.compiler.operationoutput.OperationDescriptorList
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

abstract class ApolloGenerateOperationIdsTask : DefaultTask() {
  @get:Internal
  @get:PathSensitive(PathSensitivity.RELATIVE)
  // This is not declared as input so we can skip the task if the file does not exist.
  // See https://github.com/gradle/gradle/issues/2919
  abstract val operationDescriptorListFile: RegularFileProperty

  @get:InputFiles
  @get:SkipWhenEmpty
  @get:PathSensitive(PathSensitivity.RELATIVE)
  val filesUsedForUpToDateChecks: FileCollection
    get() {
      val f = operationDescriptorListFile.get().asFile
      return project.files().apply {
        if (f.exists()) {
          from(f)
        }
      }
    }

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