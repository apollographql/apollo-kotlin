package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.codegen.writeTo
import com.apollographql.apollo3.compiler.toCodegenMetadata
import com.apollographql.apollo3.compiler.toCodegenOptions
import com.apollographql.apollo3.compiler.toCodegenSchema
import com.apollographql.apollo3.compiler.toIrOperations
import org.gradle.api.file.ConfigurableFileCollection
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
  abstract val downstreamUsedCoordinates: MapProperty<String, Set<String>>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamMetadata: ConfigurableFileCollection

  @get:OutputFile
  @get:Optional
  abstract val metadataOutputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val codegenSchemaFile = codegenSchemas.findCodegenSchemaFile()

    ApolloCompiler.buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchemaFile.toCodegenSchema(),
        irOperations = irOperations.get().asFile.toIrOperations(),
        downstreamUsedCoordinates = downstreamUsedCoordinates.get(),
        upstreamCodegenMetadata = upstreamMetadata.files.map { it.toCodegenMetadata() },
        codegenOptions = codegenOptionsFile.get().asFile.toCodegenOptions(),
        layout = null,
        compilerKotlinHooks = compilerKotlinHooks,
        compilerJavaHooks = compilerJavaHooks,
        operationManifestFile = operationManifestFile.orNull?.asFile,
        operationOutputGenerator = operationOutputGenerator
    ).writeTo(outputDir.get().asFile, true, metadataOutputFile.orNull?.asFile)
  }

  companion object {
    fun Iterable<File>.findCodegenSchemaFile(): File {
      return firstOrNull {
        it.length() > 0
      } ?: error("Cannot find CodegenSchema in $this")
    }
  }
}
