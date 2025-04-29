package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.LayoutFactory
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo.compiler.toCodegenOptions
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@CacheableTask
abstract class ApolloGenerateDataBuildersSourcesTask : ApolloTaskWithClasspath(), ApolloGenerateDataBuildersSourcesBaseTask {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemas: ConfigurableFileCollection

  @get:Input
  abstract val downstreamUsedCoordinates: MapProperty<String, Map<String, Set<String>>>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamMetadata: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenOptionsFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val workQueue = getWorkQueue()

    workQueue.submit(GenerateDataBuildersWorkAction::class.java) {
      it.codegenSchemas.from(codegenSchemas)
      it.codegenOptions.set(codegenOptionsFile)
      it.downstreamUsedCoordinates.set(downstreamUsedCoordinates)
      it.upstreamMetadata.from(upstreamMetadata)
      it.outputDir.set(dataBuildersOutputDir)
      it.hasPlugin = hasPlugin.get()
      it.arguments = arguments.get()
      it.logLevel = logLevel.get().ordinal
      it.apolloBuildService.set(apolloBuildService)
      it.classpath = classpath
    }
  }
}

private abstract class GenerateDataBuildersWorkAction : WorkAction<GenerateDataBuildersSources> {
  override fun execute() {
    with(parameters) {
      runInIsolation(apolloBuildService.get(), classpath) {
        it.reflectiveCall(
            "buildDataBuilders",
            arguments,
            logLevel,
            hasPlugin,
            codegenSchemas.toInputFiles().isolate(),
            upstreamMetadata.toInputFiles().isolate(),
            downstreamUsedCoordinates.get(),
            codegenOptions.get().asFile,
            outputDir.get().asFile,
        )
      }
    }
  }
}

private interface GenerateDataBuildersSources : WorkParameters {
  var hasPlugin: Boolean
  val codegenSchemas: ConfigurableFileCollection
  val codegenOptions: RegularFileProperty
  val downstreamUsedCoordinates: MapProperty<String, Map<String, Set<String>>>
  val upstreamMetadata: ConfigurableFileCollection
  val outputDir: DirectoryProperty
  var arguments: Map<String, Any?>
  var logLevel: Int
  val apolloBuildService: Property<ApolloBuildService>
  var classpath: FileCollection
}