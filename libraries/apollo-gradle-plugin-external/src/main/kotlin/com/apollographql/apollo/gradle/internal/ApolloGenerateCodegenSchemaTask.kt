package com.apollographql.apollo.gradle.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.util.function.Consumer

@CacheableTask
abstract class ApolloGenerateCodegenSchemaTask : ApolloTaskWithClasspath() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val fallbackSchemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamSchemaFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  @get:OutputFile
  @get:Optional
  abstract val codegenSchemaFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    if (upstreamSchemaFiles.files.isNotEmpty()) {
      /**
       * Output an empty file
       */
      codegenSchemaFile.get().asFile.let {
        it.delete()
        it.createNewFile()
      }
      return
    }

    val workQueue = getWorkQueue()

    workQueue.submit(GenerateCodegenSchema::class.java) {
      it.codegenSchemaFile = codegenSchemaFile
      it.schemaFiles = schemaFiles.isolate()
      it.fallbackSchemaFiles = fallbackSchemaFiles.isolate()
      it.codegenSchemaOptionsFile.set(codegenSchemaOptionsFile)
      it.hasPlugin = hasPlugin.get()
      it.arguments = arguments.get()
      it.logLevel = logLevel.get().ordinal
      it.apolloBuildService.set(apolloBuildService)
      it.classpath = classpath
    }
  }
}


private abstract class GenerateCodegenSchema : WorkAction<GenerateCodegenSchemaParameters> {
  override fun execute() {
    with(parameters) {
      runInIsolation(apolloBuildService.get(), classpath) {
        it.reflectiveCall(
            "buildCodegenSchema",
            arguments,
            logLevel,
            hasPlugin,
            (schemaFiles.takeIf { it.isNotEmpty() } ?: fallbackSchemaFiles),
            warningMessageConsumer,
            codegenSchemaOptionsFile.get().asFile,
            codegenSchemaFile.get().asFile
        )
      }
    }
  }
}

private interface GenerateCodegenSchemaParameters : WorkParameters {
  var codegenSchemaFile: RegularFileProperty
  var schemaFiles: List<Any>
  var fallbackSchemaFiles: List<Any>
  val codegenSchemaOptionsFile: RegularFileProperty
  var hasPlugin: Boolean
  var arguments: Map<String, Any?>
  var logLevel: Int
  val apolloBuildService: Property<ApolloBuildService>
  var classpath: FileCollection
}

