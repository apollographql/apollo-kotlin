package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ApolloGenerateSourcesTask : ApolloGenerateSourcesBaseTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOptionsFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    ApolloCompiler.compile(
        schemaFiles = schemaFiles.files,
        executableFiles = graphqlFiles.files,
        codegenSchemaOptionsFile = codegenSchemaOptionsFile.get().asFile,
        codegenOptionsFile = codegenOptionsFile.get().asFile,
        irOptionsFile = irOptionsFile.get().asFile,
        logger = logger(),
        packageNameGenerator = packageNameGenerator,
        packageNameRoots = packageNameRoots,
        operationOutputGenerator = operationOutputGenerator,
        compilerJavaHooks = compilerJavaHooks,
        compilerKotlinHooks = compilerKotlinHooks,
        outputDir = outputDir.get().asFile,
        codegenMetadataFile = null,
        operationManifestFile = operationManifestFile.orNull?.asFile
    )
  }
}
