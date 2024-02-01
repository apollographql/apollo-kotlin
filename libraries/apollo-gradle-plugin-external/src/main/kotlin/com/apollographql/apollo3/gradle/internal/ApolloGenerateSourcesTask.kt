package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.codegen.writeTo
import com.apollographql.apollo3.compiler.toCodegenOptions
import com.apollographql.apollo3.compiler.toCodegenSchemaOptions
import com.apollographql.apollo3.compiler.toIrOptions
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import com.apollographql.apollo3.compiler.InputFile as ApolloInputFile

@CacheableTask
abstract class ApolloGenerateSourcesTask : ApolloGenerateSourcesBaseTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @Internal
  var sourceRoots: Set<String>? = null

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val fallbackSchemaFiles: ConfigurableFileCollection

  @get:org.gradle.api.tasks.InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  @get:org.gradle.api.tasks.InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOptionsFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    if (requiresBuildscriptClasspath()) {
      val normalizedSchemaFiles = (schemaFiles.files.takeIf { it.isNotEmpty() } ?: fallbackSchemaFiles.files).map {
        // this may produce wrong cache results as that computation is not the same as the Gradle normalization
        ApolloInputFile(it, it.normalizedPath(sourceRoots!!))
      }
      val normalizedExecutableFiles = graphqlFiles.files.map {
        ApolloInputFile(it, it.normalizedPath(sourceRoots!!))
      }

      ApolloCompiler.buildSchemaAndOperationsSources(
          schemaFiles = normalizedSchemaFiles,
          executableFiles = normalizedExecutableFiles,
          codegenSchemaOptions = codegenSchemaOptionsFile.get().asFile.toCodegenSchemaOptions(),
          codegenOptions = codegenOptionsFile.get().asFile.toCodegenOptions(),
          irOptions = irOptionsFile.get().asFile.toIrOptions(),
          logger = logger(),
          layout = layout(),
          operationOutputGenerator = operationOutputGenerator,
          compilerJavaHooks = compilerJavaHooks,
          compilerKotlinHooks = compilerKotlinHooks,
          irOperationsTransform = null,
          javaOutputTransform = null,
          kotlinOutputTransform = null,
          operationManifestFile = operationManifestFile.orNull?.asFile
      ).writeTo(outputDir.get().asFile, true, null)
    } else {
      val workQueue = getWorkerExecutor().classLoaderIsolation { workerSpec ->
        workerSpec.classpath.from(classpath)
      }

      workQueue.submit(GenerateSources::class.java) {
        it.graphqlFiles.from(graphqlFiles.files)
        it.schemaFiles.from(schemaFiles)
        it.fallbackSchemaFiles.from(fallbackSchemaFiles)
        it.sourceRoots = sourceRoots!!
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
      val normalizedSchemaFiles = (schemaFiles.files.takeIf { it.isNotEmpty() } ?: fallbackSchemaFiles.files).map {
        // this may produce wrong cache results as that computation is not the same as the Gradle normalization
        ApolloInputFile(it, it.normalizedPath(sourceRoots))
      }
      val normalizedExecutableFiles = graphqlFiles.files.map {
        ApolloInputFile(it, it.normalizedPath(sourceRoots))
      }

      val plugin = apolloCompilerPlugin()

      ApolloCompiler.buildSchemaAndOperationsSources(
          schemaFiles = normalizedSchemaFiles,
          executableFiles = normalizedExecutableFiles,
          codegenSchemaOptions = codegenSchemaOptions.get().asFile.toCodegenSchemaOptions(),
          codegenOptions = codegenOptions.get().asFile.toCodegenOptions(),
          irOptions = irOptions.get().asFile.toIrOptions(),
          logger = logger(),
          layout = { plugin?.layout(it) },
          operationOutputGenerator = plugin?.operationOutputGenerator(),
          compilerJavaHooks = null,
          compilerKotlinHooks = null,
          irOperationsTransform = plugin?.irOperationsTransform(),
          javaOutputTransform = plugin?.javaOutputTransform(),
          kotlinOutputTransform = plugin?.kotlinOutputTransform(),
          operationManifestFile = operationManifestFile.orNull?.asFile
      ).writeTo(outputDir.get().asFile, true, null)
    }
  }
}

private interface GenerateSourcesParameters : WorkParameters {
  val graphqlFiles: ConfigurableFileCollection
  val schemaFiles: ConfigurableFileCollection
  val fallbackSchemaFiles: ConfigurableFileCollection
  var sourceRoots: Set<String>
  val codegenSchemaOptions: RegularFileProperty
  val codegenOptions: RegularFileProperty
  val irOptions: RegularFileProperty
  val operationManifestFile: RegularFileProperty
  val outputDir: DirectoryProperty
}

