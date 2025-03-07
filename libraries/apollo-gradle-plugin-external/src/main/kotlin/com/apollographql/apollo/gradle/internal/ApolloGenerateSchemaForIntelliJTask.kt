package com.apollographql.apollo.gradle.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@CacheableTask
abstract class ApolloGenerateSchemaForIJTask : ApolloTaskWithClasspath() {

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemas: ConfigurableFileCollection

  @get:OutputFile
  abstract val fullSchema: RegularFileProperty

  @get:OutputFile
  abstract val linkedDefinitions: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val workQueue = getWorkQueue()

    workQueue.submit(GenerateSchemaForIntelliJ::class.java) {
      it.upstreamCodegenSchemas = codegenSchemas.isolate()
      it.fullSchema = fullSchema
      it.linkedDefinitions = linkedDefinitions

      it.hasPlugin = hasPlugin.get()
      it.logLevel = logLevel.get().ordinal
      it.apolloBuildService.set(apolloBuildService)
      it.classpath = classpath
    }
  }
}


private abstract class GenerateSchemaForIntelliJ : WorkAction<GenerateSchemaForIntelliJParameters> {
  override fun execute() {
    with(parameters) {
      runInIsolation(apolloBuildService.get(), classpath) {
        it.reflectiveCall(
            "buildSchemaForIntelliJ",
            upstreamCodegenSchemas,
            fullSchema.get().asFile,
            linkedDefinitions.get().asFile
        )
      }
    }
  }
}

private interface GenerateSchemaForIntelliJParameters : WorkParameters {
  var upstreamCodegenSchemas: List<Any>
  var fullSchema: RegularFileProperty
  var linkedDefinitions: RegularFileProperty
  var hasPlugin: Boolean
  var logLevel: Int
  val apolloBuildService: Property<ApolloBuildService>
  var classpath: FileCollection
}

