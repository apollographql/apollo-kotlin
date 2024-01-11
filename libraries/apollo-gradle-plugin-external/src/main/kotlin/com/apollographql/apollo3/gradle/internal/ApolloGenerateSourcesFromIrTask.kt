package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class ApolloGenerateSourcesFromIrTask : ApolloGenerateSourcesBaseTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemas: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOperations: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  abstract val irSchema: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamMetadata: ConfigurableFileCollection

  @get:OutputFile
  @get:Optional
  abstract val metadataOutputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val codegenSchemaFile = codegenSchemas.findCodegenSchemaFile()

    ApolloCompiler.buildSchemaAndOperationSources(
        codegenSchemaFile = codegenSchemaFile,
        irOperationsFile = irOperations.get().asFile,
        irSchemaFile = irSchema.orNull?.asFile,
        upstreamCodegenMetadataFiles = upstreamMetadata.files,
        codegenOptionsFile = codegenOptionsFile.get().asFile,
        packageNameGenerator = packageNameGenerator,
        packageNameRoots = packageNameRoots,
        compilerKotlinHooks = compilerKotlinHooks,
        compilerJavaHooks = compilerJavaHooks,
        sourcesDir = outputDir.get().asFile,
        operationManifestFile = operationManifestFile.orNull?.asFile,
        codegenMetadataFile = metadataOutputFile.orNull?.asFile
    )
  }

  companion object {
    fun Iterable<File>.findCodegenSchemaFile(): File {
      return firstOrNull {
        it.length() > 0
      } ?: error("Cannot find CodegenSchema in $this")
    }
  }
}
