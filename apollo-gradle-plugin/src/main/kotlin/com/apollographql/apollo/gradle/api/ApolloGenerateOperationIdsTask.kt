package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.compiler.operationoutput.OperationList
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ApolloGenerateOperationIdsTask : DefaultTask() {
  @get:Internal
  // We do not declare this as input so we can skip the task if the file does not exist.
  // See https://github.com/gradle/gradle/issues/2919
  abstract val operationList: RegularFileProperty

  @get:InputFiles
  @get:SkipWhenEmpty
  @get:PathSensitive(PathSensitivity.RELATIVE)
  val filesUsedForUpToDateChecks: FileCollection
    get() {
      val f = operationList.get().asFile
      return project.files().apply {
        if (f.exists()) {
          from(f)
        }
      }
    }

  @get:OutputFile
  abstract val operationOutput: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val operationList = OperationList(operationList.get().asFile)
    operationOutput.get().asFile.writeText(generateOperationOutput(operationList).toJson())
  }

  abstract fun generateOperationOutput(operationList: OperationList): OperationOutput
}