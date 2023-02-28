package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.defaultGenerateDataBuilders
import com.apollographql.apollo3.compiler.writeTo
import com.apollographql.apollo3.gradle.internal.ApolloGenerateSourcesTask.Companion.scalarMapping
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ApolloGenerateSchemaTask : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val outputFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:Input
  @get:Optional
  abstract val scalarTypeMapping: MapProperty<String, String>

  @get:Input
  @get:Optional
  abstract val scalarAdapterMapping: MapProperty<String, String>

  @get:Internal
  lateinit var packageNameGenerator: PackageNameGenerator

  @Input
  fun getPackageNameGeneratorVersion() = packageNameGenerator.version

  @get:Input
  abstract val codegenModels: Property<String>

  @get:Input
  abstract val targetLanguage: Property<TargetLanguage>

  @get:Internal
  abstract val userGenerateKotlinModels: Property<Boolean>

  @get:Internal
  abstract val userCodegenModels: Property<String>

  @get:Input
  @get:Optional
  abstract val generateDataBuilders: Property<Boolean>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamSchemaFiles: ConfigurableFileCollection

  @TaskAction
  fun taskAction() {
    if (upstreamSchemaFiles.files.isNotEmpty()) {
      /**
       * We already have a schema
       */
      check(!generateDataBuilders.isPresent) {
        "Apollo: generateDataBuilders cannot be used because this module depends on another one that has already set generateDataBuilders"
      }
      check(scalarTypeMapping.get().isEmpty()) {
        "Apollo: scalarTypeMapping is not used because this module depends on another one that has already set scalarTypeMapping"
      }
      check(!userCodegenModels.isPresent) {
        "Apollo: codegenModels is not used because this module depends on another one that has already set codegenModels"
      }
      check(!userGenerateKotlinModels.isPresent) {
        "Apollo: generateKotlinModels is not used because this module depends on another one that has already set targetLanguage"
      }
      check(schemaFiles.isEmpty) {
        "Apollo: this module depends on another one that already has a schema. Double check that no schema file is present in this module and/or that schemaFile(s) is not specified in Gradle configuration"
      }

      outputFile.get().asFile.let {
        it.delete()
        it.createNewFile()
      }
      return
    }

    val codegenSchema = ApolloCompiler.buildCodegenSchema(
        schemaFiles = schemaFiles.files,
        codegenModels = codegenModels.get(),
        packageNameGenerator = packageNameGenerator,
        scalarMapping = scalarMapping(scalarTypeMapping, scalarAdapterMapping),
        logger = logger(),
        targetLanguage = targetLanguage.get(),
        generateDataBuilders = generateDataBuilders.getOrElse(defaultGenerateDataBuilders)
    )

    codegenSchema.writeTo(outputFile.get().asFile)
  }
}
