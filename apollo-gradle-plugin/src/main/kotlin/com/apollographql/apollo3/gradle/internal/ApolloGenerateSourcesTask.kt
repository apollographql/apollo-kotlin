package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloMetadata
import com.apollographql.apollo3.compiler.ApolloMetadata.Companion.merge
import com.apollographql.apollo3.compiler.GraphQLCompiler
import com.apollographql.apollo3.compiler.IncomingOptions
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.Options
import com.apollographql.apollo3.compiler.Options.Companion.defaultAlwaysGenerateTypesMatching
import com.apollographql.apollo3.compiler.Options.Companion.defaultCodegenModels
import com.apollographql.apollo3.compiler.Options.Companion.defaultFailOnWarnings
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateAsInternal
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateFilterNotNull
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateFragmentImplementations
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateQueryDocument
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateResponseFields
import com.apollographql.apollo3.compiler.Options.Companion.defaultUseSemanticNaming
import com.apollographql.apollo3.compiler.Options.Companion.defaultWarnOnDeprecatedUsages
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.Roots
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
  @get:Optional
  abstract val metadataOutputFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  /**
   * It's ok to have schemaFiles empty if there is some metadata
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val metadataFiles: ConfigurableFileCollection

  @get:Input
  abstract val rootFolders: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val alwaysGenerateTypesMatching: SetProperty<String>

  @get:Internal
  lateinit var packageNameGenerator: PackageNameGenerator
  @Input
  fun getPackageNameGeneratorVersion() = packageNameGenerator.version

  @get:Internal
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

  @get:Input
  @get:Optional
  abstract val generateQueryDocument: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateResponseFields: Property<Boolean>

  @get:Internal
  abstract val warnOnDeprecatedUsages: Property<Boolean>

  @get:Internal
  abstract val failOnWarnings: Property<Boolean>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:OutputDirectory
  @get:Optional
  abstract val debugDir: DirectoryProperty

  @get:Input
  @get:Optional
  abstract val generateAsInternal: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateFilterNotNull: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateFragmentImplementations: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val codegenModels: Property<String>

  @get:Input
  @get:Optional
  abstract val flattenModels: Property<Boolean>

  @get:Inject
  abstract val objectFactory: ObjectFactory

  @get:Input
  abstract val projectName: Property<String>

  @TaskAction
  fun taskAction() {
    val roots = Roots(objectFactory.fileCollection().from(rootFolders).files.toList())
    val schemaFiles = schemaFiles.files
    val metadata = metadataFiles.files.toList().map { ApolloMetadata.readFrom(it) }.merge()

    val incomingOptions = if (metadata != null) {
      check(schemaFiles.isEmpty()) {
        "Specifying 'schemaFiles' has no effect as an upstream module already provided a schema"
      }
      check(!customScalarsMapping.isPresent) {
        "Specifying 'customScalarsMapping' has no effect as an upstream module already provided a customScalarsMapping"
      }
      check(!codegenModels.isPresent) {
        "Specifying 'codegenModels' has no effect as an upstream module already provided a codegenModels"
      }
      check(!flattenModels.isPresent) {
        "Specifying 'flattenModels' has no effect as an upstream module already provided a flattenModels"
      }
      IncomingOptions.fromMetadata(metadata)
    } else {
      val codegenModels = codegenModels.getOrElse(defaultCodegenModels)
      // Response-based models generate a lot of models and therefore a lot of name clashes if flattened
      val defaultFlattenModels = flattenModels.getOrElse(codegenModels != MODELS_RESPONSE_BASED)

      check(schemaFiles.isNotEmpty()) {
        "No schema file found in:\n${rootFolders.get().joinToString("\n")}"
      }
      IncomingOptions.fromOptions(
          schemaFiles = schemaFiles,
          customScalarsMapping = customScalarsMapping.getOrElse(emptyMap()),
          codegenModels = codegenModels,
          flattenModels = flattenModels.getOrElse(defaultFlattenModels),
          packageNameGenerator = packageNameGenerator
      )
    }

    val logger = object : GraphQLCompiler.Logger {
      override fun warning(message: String) {
        logger.lifecycle(message)
      }
    }

    val options = Options(
        executableFiles = graphqlFiles.files,
        outputDir = outputDir.asFile.get(),
        debugDir = debugDir.asFile.orNull,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching.getOrElse(defaultAlwaysGenerateTypesMatching),
        operationOutputFile = operationOutputFile.asFile.orNull,
        operationOutputGenerator = operationOutputGenerator,
        useSemanticNaming = useSemanticNaming.getOrElse(defaultUseSemanticNaming),
        warnOnDeprecatedUsages = warnOnDeprecatedUsages.getOrElse(defaultWarnOnDeprecatedUsages),
        failOnWarnings = failOnWarnings.getOrElse(defaultFailOnWarnings),
        packageNameGenerator = packageNameGenerator,
        generateAsInternal = generateAsInternal.getOrElse(defaultGenerateAsInternal),
        generateFilterNotNull = generateFilterNotNull.getOrElse(defaultGenerateFilterNotNull),
        generateFragmentImplementations = generateFragmentImplementations.getOrElse(defaultGenerateFragmentImplementations),
        generateQueryDocument = generateQueryDocument.getOrElse(defaultGenerateQueryDocument),
        generateResponseFields = generateResponseFields.getOrElse(defaultGenerateResponseFields),
        logger = logger,
        metadataOutputFile = metadataOutputFile.asFile.orNull,
        moduleName = projectName.get(),
        incomingOptions = incomingOptions
    )

    GraphQLCompiler.write(options)
  }
}
