package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.toCodegenSchemaOptions
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ApolloGenerateCodegenSchemaTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamSchemaFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  /**
   * Only used for checks
   */
  @get:Input
  @get:Optional
  abstract val userGenerateKotlinModels: Property<Boolean>

  /**
   * Only used for checks
   */
  @get:Input
  @get:Optional
  abstract val userCodegenModels: Property<String>

  @get:OutputFile
  @get:Optional
  abstract val codegenSchemaFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    if (upstreamSchemaFiles.files.isNotEmpty()) {
      val options = codegenSchemaOptionsFile.get().asFile.toCodegenSchemaOptions()
      /**
       * We already have a schema
       */
      check(schemaFiles.isEmpty) {
        "Apollo: this module depends on another one that already has a schema. Double check that no schema file is present in this module and/or that schemaFile(s) is not specified in Gradle configuration"
      }
      check(options.generateDataBuilders == null) {
        "Apollo: generateDataBuilders cannot be used because this module depends on another one that has already set generateDataBuilders"
      }
      check(options.scalarMapping.isEmpty()) {
        "Apollo: scalarTypeMapping is not used because this module depends on another one that has already set scalarTypeMapping"
      }
      check(!userCodegenModels.isPresent) {
        "Apollo: codegenModels is not used because this module depends on another one that has already set codegenModels"
      }
      check(!userGenerateKotlinModels.isPresent) {
        "Apollo: generateKotlinModels is not used because this module depends on another one that has already set targetLanguage"
      }

      /**
       * Output an empty file
       */
      codegenSchemaFile.get().asFile.let {
        it.delete()
        it.createNewFile()
      }
      return
    }

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = schemaFiles.files,
        logger = logger(),
        codegenSchemaOptionsFile = codegenSchemaOptionsFile.get().asFile,
        codegenSchemaFile = codegenSchemaFile.get().asFile,
    )
  }
}
