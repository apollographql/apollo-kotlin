package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.codegen.writeTo
import com.apollographql.apollo3.compiler.toCodegenOptions
import com.apollographql.apollo3.compiler.toCodegenSchemaOptions
import com.apollographql.apollo3.compiler.toIrOptions
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
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
    val normalizedSchemaFiles = (schemaFiles.files.takeIf { it.isNotEmpty() }?: fallbackSchemaFiles.files).map {
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
        operationManifestFile = operationManifestFile.orNull?.asFile
    ).writeTo(outputDir.get().asFile, true, null)
  }
}


