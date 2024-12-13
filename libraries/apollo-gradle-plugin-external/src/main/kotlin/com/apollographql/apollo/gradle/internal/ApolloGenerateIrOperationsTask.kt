package com.apollographql.apollo.gradle.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.util.function.Consumer

@CacheableTask
abstract class ApolloGenerateIrOperationsTask : ApolloTaskWithClasspath() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamIrFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOptionsFile: RegularFileProperty

  @get:OutputFile
  abstract val irOperationsFile: RegularFileProperty

  @TaskAction
  fun taskAction() {

    val workQueue = getWorkQueue()

    workQueue.submit(GenerateIrOperations::class.java) {
      it.codegenSchemaFiles = codegenSchemaFiles.isolate()
      it.graphqlFiles = graphqlFiles.isolate()
      it.upstreamIrFiles = upstreamIrFiles.isolate()
      it.irOptionsFile.set(irOptionsFile)
      it.irOperationsFile.set(irOperationsFile)
      it.arguments = arguments.get()
      it.logLevel = logLevel.get().ordinal
      it.apolloBuildService.set(apolloBuildService)
      it.classpath = classpath
    }
  }
}

private abstract class GenerateIrOperations : WorkAction<GenerateIrOperationsParameters> {
  override fun execute() {
    with(parameters) {
      runInIsolation(apolloBuildService.get(), classpath) {
        it.reflectiveCall(
            "buildIr",
            arguments,
            logLevel,
            graphqlFiles,
            codegenSchemaFiles,
            upstreamIrFiles,
            irOptionsFile.get().asFile,
            warningMessageConsumer,
            irOperationsFile.get().asFile
        )
      }
    }
  }
}

private interface GenerateIrOperationsParameters : WorkParameters {
  var codegenSchemaFiles: List<Any>
  var graphqlFiles: List<Any>
  var upstreamIrFiles: List<Any>
  val irOptionsFile: RegularFileProperty
  val irOperationsFile: RegularFileProperty
  var arguments: Map<String, Any?>
  var logLevel: Int
  val apolloBuildService: Property<ApolloBuildService>
  var classpath: FileCollection
}
