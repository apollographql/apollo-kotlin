package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.codegen.writeTo
import com.apollographql.apollo3.compiler.toCodegenOptions
import com.apollographql.apollo3.compiler.toCodegenSchemaOptions
import com.apollographql.apollo3.compiler.toIrOptions
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
    ApolloCompiler.buildSchemaAndOperationsSources(
        schemaFiles = schemaFiles.files,
        executableFiles = graphqlFiles.files,
        codegenSchemaOptions = codegenSchemaOptionsFile.get().asFile.toCodegenSchemaOptions(),
        codegenOptions = codegenOptionsFile.get().asFile.toCodegenOptions(),
        irOptions = irOptionsFile.get().asFile.toIrOptions(),
        logger = logger(),
        packageNameGenerator = packageNameGenerator(),
        operationOutputGenerator = operationOutputGenerator,
        compilerJavaHooks = compilerJavaHooks,
        compilerKotlinHooks = compilerKotlinHooks,
        operationManifestFile = operationManifestFile.orNull?.asFile
    ).writeTo(outputDir.get().asFile, true, null)
  }
}
