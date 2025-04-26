@file:Suppress("DEPRECATION")

package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.ApolloCompiler
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.LayoutFactory
import com.apollographql.apollo.compiler.PackageNameGenerator
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.findCodegenSchemaFile
import com.apollographql.apollo.compiler.toCodegenMetadata
import com.apollographql.apollo.compiler.toCodegenOptions
import com.apollographql.apollo.compiler.toCodegenSchema
import com.apollographql.apollo.compiler.toUsedCoordinates
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
import org.gradle.api.tasks.Internal
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

  @Suppress("DEPRECATION")
  @get:Internal
  var packageNameGenerator: PackageNameGenerator? = null

  @Input
  fun getPackageNameGeneratorVersion() = packageNameGenerator?.version ?: ""

  @TaskAction
  fun taskAction() {
    if (packageNameGenerator != null) {
      logger.lifecycle("Apollo: packageNameGenerator is deprecated, use Apollo compiler plugins instead. See https://go.apollo.dev/ak-compiler-plugins for more details.")

      val codegenSchemaFile = codegenSchemas.files.findCodegenSchemaFile()

      ApolloCompiler.buildDataBuilders(
          codegenSchema = codegenSchemaFile.toCodegenSchema(),
          upstreamCodegenMetadata = upstreamMetadata.files.map { it.toCodegenMetadata() },
          codegenOptions = codegenOptionsFile.get().asFile.toCodegenOptions(),
          layout = layout().create(codegenSchemaFile.toCodegenSchema()),
          usedCoordinates = downstreamUsedCoordinates.get().toUsedCoordinates(),
      ).writeTo(dataBuildersOutputDir.get().asFile, true, null)
    } else {
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
}

private fun ApolloGenerateDataBuildersSourcesTask.layout(): LayoutFactory {
  return object : LayoutFactory {
    override fun create(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
      return if (packageNameGenerator == null) {
        null
      } else {
        val options = codegenOptionsFile.get().asFile.toCodegenOptions()
        SchemaAndOperationsLayout(codegenSchema, packageNameGenerator!!, options.useSemanticNaming, options.decapitalizeFields, options.generatedSchemaName)
      }
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