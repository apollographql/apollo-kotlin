package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.ApolloCompiler
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.toCodegenOptions
import com.apollographql.apollo.compiler.toCodegenSchema
import com.apollographql.apollo.compiler.toIrOptions
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@CacheableTask
abstract class ApolloGenerateSourcesTask : ApolloGenerateSourcesBaseTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchema: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOptionsFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    if (requiresBuildscriptClasspath()) {
      val executableInputFiles = graphqlFiles.toInputFiles()

      val codegenSchema = codegenSchema.get().asFile.toCodegenSchema()

      ApolloCompiler.buildSchemaAndOperationsSources(
          codegenSchema = codegenSchema,
          executableFiles = executableInputFiles,
          codegenOptions = codegenOptionsFile.get().asFile.toCodegenOptions(),
          irOptions = irOptionsFile.get().asFile.toIrOptions(),
          logger = logger(),
          layoutFactory = layout(),
          operationOutputGenerator = operationOutputGenerator,
          irOperationsTransform = null,
          javaOutputTransform = null,
          kotlinOutputTransform = null,
          documentTransform = null,
          operationManifestFile = operationManifestFile.orNull?.asFile
      ).writeTo(outputDir.get().asFile, true, null)
    } else {
      val workQueue = getWorkQueue()

      workQueue.submit(GenerateSources::class.java) {
        it.hasPlugin = hasPlugin.get()
        it.graphqlFiles = graphqlFiles.isolate()
        it.irOptions.set(irOptionsFile)
        it.codegenSchema.set(codegenSchema)
        it.codegenOptions.set(codegenOptionsFile)
        it.operationManifestFile.set(operationManifestFile)
        it.outputDir.set(outputDir)
        it.arguments = arguments.get()
        it.logLevel = logLevel.get().ordinal
        it.apolloBuildService.set(apolloBuildService)
        it.classpath = classpath
      }
    }
  }
}

private abstract class GenerateSources : WorkAction<GenerateSourcesParameters> {
  override fun execute() {
    with(parameters) {
      runInIsolation(apolloBuildService.get(), classpath) {
        it.reflectiveCall("buildSources",
                arguments,
                logLevel,
                hasPlugin,
                codegenSchema.get().asFile,
                graphqlFiles,
                codegenOptions.get().asFile,
                irOptions.get().asFile,
                warningMessageConsumer,
                operationManifestFile.orNull?.asFile,
                outputDir.get().asFile
            )
      }
    }
  }
}

private interface GenerateSourcesParameters : WorkParameters {
  var hasPlugin: Boolean
  var graphqlFiles: List<Any>
  val codegenSchema: RegularFileProperty
  val codegenOptions: RegularFileProperty
  val irOptions: RegularFileProperty
  val operationManifestFile: RegularFileProperty
  val outputDir: DirectoryProperty
  var arguments: Map<String, Any?>
  var logLevel: Int
  val apolloBuildService: Property<ApolloBuildService>
  var classpath: FileCollection
}
