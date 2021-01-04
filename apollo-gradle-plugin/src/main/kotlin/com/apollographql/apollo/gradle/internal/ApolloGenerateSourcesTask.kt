package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.OperationOutputGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class ApolloGenerateSourcesTask : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val operationOutputFile: RegularFileProperty

  @get:OutputFile
  abstract val metadataOutputFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  /**
   * It's ok to have schemaFile = null if there is some metadata
   */
  @get:InputFile
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val metadataFiles: ConfigurableFileCollection

  @get:Input
  abstract val rootFolders: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val alwaysGenerateTypesMatching: SetProperty<String>

  @get:Input
  @get:Optional
  abstract val rootPackageName: Property<String>

  @get: Internal
  lateinit var operationOutputGenerator: OperationOutputGenerator

  @Input
  fun getOperationOutputGeneratorVersion() = operationOutputGenerator.version

  @get:Input
  @get:Optional
  abstract val customScalarsMapping: MapProperty<String, String>

  @get:Input
  @get:Optional
  abstract val useSemanticNaming: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateKotlinModels: Property<Boolean>

  @get:Internal
  abstract val warnOnDeprecatedUsages: Property<Boolean>

  @get:Internal
  abstract val failOnWarnings: Property<Boolean>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Input
  @get:Optional
  abstract val generateAsInternal: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateFilterNotNull: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val sealedClassesForEnumsMatching: ListProperty<String>

  @get:Inject
  abstract val objectFactory: ObjectFactory

  @get:Input
  abstract val projectName: Property<String>

  @TaskAction
  fun taskAction() {
    val args = GraphQLCompiler.Arguments(
        rootFolders = objectFactory.fileCollection().from(rootFolders).files.toList(),
        graphqlFiles = graphqlFiles.files,
        schemaFile = schemaFile.asFile.orNull,
        outputDir = outputDir.asFile.get(),

        metadata = metadataFiles.files.toList(),
        metadataOutputFile = metadataOutputFile.asFile.get(),
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching.getOrElse(emptySet()),
        moduleName = projectName.get(),

        operationOutputFile = operationOutputFile.asFile.orNull,
        operationOutputGenerator = operationOutputGenerator,

        rootPackageName = rootPackageName.getOrElse(""),

        customScalarsMapping = customScalarsMapping.getOrElse(emptyMap()),
        useSemanticNaming = useSemanticNaming.getOrElse(true),
        generateKotlinModels = generateKotlinModels.getOrElse(false),
        warnOnDeprecatedUsages = warnOnDeprecatedUsages.getOrElse(true),
        failOnWarnings = failOnWarnings.getOrElse(false),
        generateAsInternal = generateAsInternal.getOrElse(false),
        generateFilterNotNull = generateFilterNotNull.getOrElse(false),
        enumAsSealedClassPatternFilters = sealedClassesForEnumsMatching.getOrElse(emptyList()).toSet(),
    )

    val logger = object :GraphQLCompiler.Logger {
      override fun warning(message: String) {
        logger.lifecycle(message)
      }
    }

    GraphQLCompiler(logger).write(args)
  }
}
