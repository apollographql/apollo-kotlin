package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.LayoutFactory
import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo3.compiler.codegen.writeTo
import com.apollographql.apollo3.compiler.toCodegenOptions
import com.apollographql.apollo3.compiler.toCodegenSchemaOptions
import com.apollographql.apollo3.compiler.toIrOptions
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File

@CacheableTask
abstract class ApolloGenerateSourcesTask : ApolloGenerateSourcesBaseTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val fallbackSchemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @get:org.gradle.api.tasks.InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  @get:org.gradle.api.tasks.InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOptionsFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    if (requiresBuildscriptClasspath()) {
      val schemaInputFiles = (schemaFiles.takeIf { it.files.isNotEmpty() } ?: fallbackSchemaFiles).toInputFiles()
      val executableInputFiles = graphqlFiles.toInputFiles()

      ApolloCompiler.buildSchemaAndOperationsSources(
          schemaFiles = schemaInputFiles,
          executableFiles = executableInputFiles,
          codegenSchemaOptions = codegenSchemaOptionsFile.get().asFile.toCodegenSchemaOptions(),
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
      val workQueue = getWorkerExecutor().classLoaderIsolation { workerSpec ->
        workerSpec.classpath.from(classpath)
      }

      workQueue.submit(GenerateSources::class.java) {
        it.graphqlFiles = graphqlFiles.isolate()
        it.schemaFiles = schemaFiles.isolate()
        it.fallbackSchemaFiles = fallbackSchemaFiles.isolate()
        it.codegenSchemaOptions.set(codegenSchemaOptionsFile)
        it.irOptions.set(irOptionsFile)
        it.codegenOptions.set(codegenOptionsFile)
        it.operationManifestFile.set(operationManifestFile)
        it.outputDir.set(outputDir)
      }
    }
  }
}

private abstract class GenerateSources : WorkAction<GenerateSourcesParameters> {
  override fun execute() {
    with(parameters) {
      val schemaInputFiles = (schemaFiles.takeIf { it.isNotEmpty() } ?: fallbackSchemaFiles).toInputFiles()
      val executableInputFiles = graphqlFiles.toInputFiles()
      val plugin = apolloCompilerPlugin()

      ApolloCompiler.buildSchemaAndOperationsSources(
          schemaFiles = schemaInputFiles,
          executableFiles = executableInputFiles,
          codegenSchemaOptions = codegenSchemaOptions.get().asFile.toCodegenSchemaOptions(),
          codegenOptions = codegenOptions.get().asFile.toCodegenOptions(),
          irOptions = irOptions.get().asFile.toIrOptions(),
          logger = logger(),
          layoutFactory = object : LayoutFactory {
            override fun create(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
              return plugin?.layout(codegenSchema)
            }
          },
          operationOutputGenerator = plugin?.toOperationOutputGenerator(),
          irOperationsTransform = plugin?.irOperationsTransform(),
          javaOutputTransform = plugin?.javaOutputTransform(),
          kotlinOutputTransform = plugin?.kotlinOutputTransform(),
          documentTransform = plugin?.documentTransform(),
          operationManifestFile = operationManifestFile.orNull?.asFile
      ).writeTo(outputDir.get().asFile, true, null)
    }
  }
}

private interface GenerateSourcesParameters : WorkParameters {
  var graphqlFiles: List<Pair<String, File>>
  var schemaFiles: List<Pair<String, File>>
  var fallbackSchemaFiles: List<Pair<String, File>>
  val codegenSchemaOptions: RegularFileProperty
  val codegenOptions: RegularFileProperty
  val irOptions: RegularFileProperty
  val operationManifestFile: RegularFileProperty
  val outputDir: DirectoryProperty
}
