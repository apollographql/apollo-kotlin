package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.ApolloCompiler
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.toCodegenMetadata
import com.apollographql.apollo.compiler.toCodegenOptions
import com.apollographql.apollo.compiler.toCodegenSchema
import com.apollographql.apollo.compiler.toIrOperations
import com.apollographql.apollo.compiler.toUsedCoordinates
import com.apollographql.apollo.gradle.internal.ApolloGenerateSourcesFromIrTask.Companion.findCodegenSchemaFile
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
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

@CacheableTask
abstract class ApolloGenerateSourcesFromIrTask : ApolloGenerateSourcesBaseTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemas: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOperations: RegularFileProperty

  @get:Input
  abstract val downstreamUsedCoordinates: MapProperty<String, Map<String, Set<String>>>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamMetadata: ConfigurableFileCollection

  @get:OutputFile
  @get:Optional
  abstract val metadataOutputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    if (requiresBuildscriptClasspath()) {
      val codegenSchemaFile = codegenSchemas.findCodegenSchemaFile()

      ApolloCompiler.buildSchemaAndOperationsSourcesFromIr(
          codegenSchema = codegenSchemaFile.toCodegenSchema(),
          irOperations = irOperations.get().asFile.toIrOperations(),
          downstreamUsedCoordinates = downstreamUsedCoordinates.get().toUsedCoordinates(),
          upstreamCodegenMetadata = upstreamMetadata.files.map { it.toCodegenMetadata() },
          codegenOptions = codegenOptionsFile.get().asFile.toCodegenOptions(),
          layout = layout().create(codegenSchemaFile.toCodegenSchema()),
          irOperationsTransform = null,
          javaOutputTransform = null,
          kotlinOutputTransform = null,
          operationManifestFile = operationManifestFile.orNull?.asFile,
          operationOutputGenerator = operationOutputGenerator
      ).writeTo(outputDir.get().asFile, true, metadataOutputFile.orNull?.asFile)
    } else {
      val workQueue = getWorkerExecutor().classLoaderIsolation { workerSpec ->
        workerSpec.classpath.from(classpath)
      }

      workQueue.submit(GenerateSourcesFromIr::class.java) {
        it.codegenSchemas.from(codegenSchemas)
        it.irOperations.set(irOperations)
        it.codegenOptions.set(codegenOptionsFile)
        it.downstreamUsedCoordinates.set(downstreamUsedCoordinates)
        it.upstreamMetadata.from(upstreamMetadata)
        it.operationManifestFile.set(operationManifestFile)
        it.outputDir.set(outputDir)
        it.metadataOutputFile.set(metadataOutputFile)
        it.hasPlugin = hasPlugin.get()
        it.arguments = arguments.get()
        it.logLevel = logLevel.get().ordinal
      }
    }
  }

  companion object {
    fun Iterable<File>.findCodegenSchemaFile(): File {
      return firstOrNull {
        it.length() > 0
      } ?: error("Cannot find CodegenSchema in $this")
    }
  }
}

private abstract class GenerateSourcesFromIr : WorkAction<GenerateSourcesFromIrParameters> {
  override fun execute() {
    with(parameters) {
      val codegenSchemaFile = codegenSchemas.findCodegenSchemaFile()

      val codegenSchema = codegenSchemaFile.toCodegenSchema()
      val plugin = apolloCompilerPlugin(
          arguments,
          logLevel,
          hasPlugin
      )

      val upstreamCodegenMetadata = upstreamMetadata.files.map { it.toCodegenMetadata() }
      ApolloCompiler.buildSchemaAndOperationsSourcesFromIr(
          codegenSchema = codegenSchema,
          irOperations = irOperations.get().asFile.toIrOperations(),
          downstreamUsedCoordinates = downstreamUsedCoordinates.get().toUsedCoordinates(),
          upstreamCodegenMetadata = upstreamCodegenMetadata,
          codegenOptions = codegenOptions.get().asFile.toCodegenOptions(),
          layout = plugin?.layout(codegenSchema),
          irOperationsTransform = plugin?.irOperationsTransform(),
          javaOutputTransform = plugin?.javaOutputTransform(),
          kotlinOutputTransform = plugin?.kotlinOutputTransform(),
          operationManifestFile = operationManifestFile.orNull?.asFile,
          operationOutputGenerator = plugin?.toOperationOutputGenerator(),
      ).writeTo(outputDir.get().asFile, true, metadataOutputFile.orNull?.asFile)

      if (upstreamCodegenMetadata.isEmpty()) {
        plugin?.schemaListener()?.let { onSchemaDocument ->
          onSchemaDocument.onSchema(codegenSchema.schema, outputDir.get().asFile)
        }
      }
    }
  }
}

private interface GenerateSourcesFromIrParameters : WorkParameters {
  var hasPlugin: Boolean
  val codegenSchemas: ConfigurableFileCollection
  val irOperations: RegularFileProperty
  val codegenOptions: RegularFileProperty
  val downstreamUsedCoordinates: MapProperty<String, Map<String, Set<String>>>
  val upstreamMetadata: ConfigurableFileCollection
  val operationManifestFile: RegularFileProperty
  val outputDir: DirectoryProperty
  val metadataOutputFile: RegularFileProperty
  var arguments: Map<String, Any?>
  var logLevel: Int
}

