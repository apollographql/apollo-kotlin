package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloMetadata
import com.apollographql.apollo3.compiler.ApolloMetadata.Companion.merge
import com.apollographql.apollo3.compiler.DefaultPackageNameProvider
import com.apollographql.apollo3.compiler.GraphQLCompiler
import com.apollographql.apollo3.compiler.GraphQLCompiler.Companion.defaultAlwaysGenerateTypesMatching
import com.apollographql.apollo3.compiler.GraphQLCompiler.Companion.defaultFailOnWarnings
import com.apollographql.apollo3.compiler.GraphQLCompiler.Companion.defaultGenerateAsInternal
import com.apollographql.apollo3.compiler.GraphQLCompiler.Companion.defaultGenerateFilterNotNull
import com.apollographql.apollo3.compiler.GraphQLCompiler.Companion.defaultGenerateFragmentImplementations
import com.apollographql.apollo3.compiler.GraphQLCompiler.Companion.defaultGenerateQueryDocument
import com.apollographql.apollo3.compiler.GraphQLCompiler.Companion.defaultGenerateResponseFields
import com.apollographql.apollo3.compiler.GraphQLCompiler.Companion.defaultUseSemanticNaming
import com.apollographql.apollo3.compiler.GraphQLCompiler.Companion.defaultWarnOnDeprecatedUsages
import com.apollographql.apollo3.compiler.OperationOutputGenerator
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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.utils.`is`
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
   * It's ok to have schemaFile = null if there is some metadata
   */
  @get:InputFile
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val extraSchemaFiles: ConfigurableFileCollection

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
  abstract val generateFragmentsAsInterfaces: Property<Boolean>

  @get:Inject
  abstract val objectFactory: ObjectFactory

  @get:Input
  abstract val projectName: Property<String>

  @TaskAction
  fun taskAction() {
    val rootPackageName = rootPackageName.getOrElse("")

    val metadata = metadataFiles.files.toList().map { ApolloMetadata.readFrom(it) }.merge()

    val roots = Roots(objectFactory.fileCollection().from(rootFolders).files.toList())

    val incomingOptions = if (metadata != null) {
      check(!schemaFile.isPresent) {
        "Specifying 'schemaFile' has no effect as an upstream module already provided a schema"
      }
      check(extraSchemaFiles.isEmpty) {
        "Specifying 'extraSchemaFiles' has no effect as an upstream module already provided a schema"
      }
      check(!customScalarsMapping.isPresent) {
        "Specifying 'customScalarsMapping' has no effect as an upstream module already provided a customScalarsMapping"
      }
      check(!generateFragmentsAsInterfaces.isPresent) {
        "Specifying 'generateFragmentsAsInterfaces' has no effect as an upstream module already provided a generateFragmentsAsInterfaces"
      }
      GraphQLCompiler.IncomingOptions.fromMetadata(metadata)
    } else {
      GraphQLCompiler.IncomingOptions.from(
          roots = roots,
          schemaFile = schemaFile.asFile.orNull ?: error("no schemaFile found"),
          extraSchemaFiles = extraSchemaFiles.files,
          customScalarsMapping = customScalarsMapping.getOrElse(emptyMap()),
          generateFragmentsAsInterfaces = generateFragmentsAsInterfaces.getOrElse(true),
          rootPackageName = rootPackageName
      )
    }

    val packageNameProvider = DefaultPackageNameProvider(
        incomingOptions.schemaPackageName,
        rootPackageName,
        roots
    )

    val logger = object :GraphQLCompiler.Logger {
      override fun warning(message: String) {
        logger.lifecycle(message)
      }
    }

    val moduleOptions = GraphQLCompiler.ModuleOptions(
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching.getOrElse(defaultAlwaysGenerateTypesMatching),
        operationOutputFile = operationOutputFile.asFile.orNull,
        operationOutputGenerator = operationOutputGenerator,
        useSemanticNaming = useSemanticNaming.getOrElse(defaultUseSemanticNaming),
        warnOnDeprecatedUsages = warnOnDeprecatedUsages.getOrElse(defaultWarnOnDeprecatedUsages),
        failOnWarnings = failOnWarnings.getOrElse(defaultFailOnWarnings),
        packageNameProvider = packageNameProvider,
        generateAsInternal = generateAsInternal.getOrElse(defaultGenerateAsInternal),
        generateFilterNotNull = generateFilterNotNull.getOrElse(defaultGenerateFilterNotNull),
        generateFragmentImplementations = generateFragmentImplementations.getOrElse(defaultGenerateFragmentImplementations),
        generateQueryDocument = generateQueryDocument.getOrElse(defaultGenerateQueryDocument),
        generateResponseFields = generateResponseFields.getOrElse(defaultGenerateResponseFields),
        logger = logger,
        metadataOutputFile = metadataOutputFile.asFile.orNull,
        moduleName = projectName.get()
    )

    GraphQLCompiler().write(
        operationFiles = graphqlFiles.files,
        outputDir = outputDir.asFile.get(),
        debugDir = debugDir.asFile.orNull,
        incomingOptions = incomingOptions,
        moduleOptions = moduleOptions,
    )
  }
}
