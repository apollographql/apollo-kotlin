package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.ApolloMetadata
import com.apollographql.apollo3.compiler.CommonMetadata
import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.IncomingOptions.Companion.resolveSchema
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.Options
import com.apollographql.apollo3.compiler.Options.Companion.defaultAddJvmOverloads
import com.apollographql.apollo3.compiler.Options.Companion.defaultAddTypename
import com.apollographql.apollo3.compiler.Options.Companion.defaultAlwaysGenerateTypesMatching
import com.apollographql.apollo3.compiler.Options.Companion.defaultClassesForEnumsMatching
import com.apollographql.apollo3.compiler.Options.Companion.defaultCodegenModels
import com.apollographql.apollo3.compiler.Options.Companion.defaultFailOnWarnings
import com.apollographql.apollo3.compiler.Options.Companion.defaultFieldsOnDisjointTypesMustMerge
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateAsInternal
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateDataBuilders
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateFilterNotNull
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateFragmentImplementations
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateModelBuilder
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateOptionalOperationVariables
import com.apollographql.apollo3.compiler.Options.Companion.defaultGeneratePrimitiveTypes
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateQueryDocument
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateResponseFields
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateSchema
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateTestBuilders
import com.apollographql.apollo3.compiler.Options.Companion.defaultGeneratedSchemaName
import com.apollographql.apollo3.compiler.Options.Companion.defaultNullableFieldStyle
import com.apollographql.apollo3.compiler.Options.Companion.defaultRequiresOptInAnnotation
import com.apollographql.apollo3.compiler.Options.Companion.defaultSealedClassesForEnumsMatching
import com.apollographql.apollo3.compiler.Options.Companion.defaultUseSchemaPackageNameForFragments
import com.apollographql.apollo3.compiler.Options.Companion.defaultUseSemanticNaming
import com.apollographql.apollo3.compiler.Options.Companion.defaultWarnOnDeprecatedUsages
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
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
  abstract val scalarTypeMapping: MapProperty<String, String>

  @get:Input
  @get:Optional
  abstract val scalarAdapterMapping: MapProperty<String, String>

  @get:Input
  @get:Optional
  abstract val useSemanticNaming: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val targetLanguage: Property<TargetLanguage>

  @get:Input
  @get:Optional
  abstract val languageVersion: Property<String>

  @get:Input
  @get:Optional
  abstract val generateModelBuilder: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateQueryDocument: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateSchema: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generatedSchemaName: Property<String>

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
  abstract val testDir: DirectoryProperty

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
  abstract val addTypename: Property<String>

  @get:Input
  @get:Optional
  abstract val flattenModels: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val sealedClassesForEnumsMatching: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val classesForEnumsMatching: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val generateOptionalOperationVariables: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateTestBuilders: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateDataBuilders: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val useSchemaPackageNameForFragments: Property<Boolean>

  @get:Inject
  abstract val objectFactory: ObjectFactory

  @get:Input
  abstract val projectName: Property<String>

  @get:Input
  @get:Optional
  abstract val addJvmOverloads: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val requiresOptInAnnotation: Property<String>

  @get:Input
  @get:Optional
  abstract val fieldsOnDisjointTypesMustMerge: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generatePrimitiveTypes: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val nullableFieldStyle: Property<JavaNullable>

  @TaskAction
  fun taskAction() {
    val metadata = metadataFiles.files.toList().map { ApolloMetadata.readFrom(it) }

    val commonMetadatas = metadata.mapNotNull { it.commonMetadata }

    check(commonMetadatas.size <= 1) {
      "Apollo: multiple schemas found in metadata"
    }

    var commonMetadata = commonMetadatas.singleOrNull()
    var rememberCommonMetadata = false

    if (commonMetadata != null) {
      check(schemaFiles.files.isEmpty()) {
        "Specifying 'schemaFiles' has no effect as an upstream module already provided a schema"
      }
      check(!codegenModels.isPresent) {
        "Specifying 'codegenModels' has no effect as an upstream module already provided a codegenModels"
      }
      check(scalarTypeMapping.getOrElse(emptyMap()).isEmpty()) {
        "Mapping scalars can only be done in the schema module"
      }
    } else {
      val codegenModels = codegenModels.getOrElse(defaultCodegenModels)
      val (schema, mainSchemaFilePath) = resolveSchema(schemaFiles.files, rootFolders.get())

      rememberCommonMetadata = true
      commonMetadata = CommonMetadata(
          schema = schema,
          codegenModels = codegenModels,
          schemaPackageName = packageNameGenerator.packageName(mainSchemaFilePath),
          pluginVersion = APOLLO_VERSION,
          scalarMapping = scalarMapping()
      )
    }

    val logger = object : ApolloCompiler.Logger {
      override fun warning(message: String) {
        logger.lifecycle(message)
      }
    }

    val targetLanguage = targetLanguage.get()
    val flattenModels = when {
      targetLanguage == TargetLanguage.JAVA -> {
        check(flattenModels.isPresent.not()) {
          "Java codegen does not support flattenModels"
        }
        true
      }
      else -> flattenModels.getOrElse(commonMetadata.codegenModels != MODELS_RESPONSE_BASED)
    }
    val codegenModels = when {
      targetLanguage == TargetLanguage.JAVA -> {
        check(commonMetadata.codegenModels == MODELS_OPERATION_BASED) {
          "Java codegen does not support codegenModels=${commonMetadata.codegenModels}"
        }
        MODELS_OPERATION_BASED
      }
      else -> commonMetadata.codegenModels
    }

    val options = Options(
        executableFiles = graphqlFiles.files,
        outputDir = outputDir.asFile.get(),
        testDir = testDir.asFile.get(),
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
        generateModelBuilder = generateModelBuilder.getOrElse(defaultGenerateModelBuilder),
        generateQueryDocument = generateQueryDocument.getOrElse(defaultGenerateQueryDocument),
        generateSchema = generateSchema.getOrElse(defaultGenerateSchema),
        generatedSchemaName = generatedSchemaName.getOrElse(defaultGeneratedSchemaName),
        generateResponseFields = generateResponseFields.getOrElse(defaultGenerateResponseFields),
        logger = logger,
        moduleName = projectName.get(),
        // Response-based models generate a lot of models and therefore a lot of name clashes if flattened
        flattenModels = flattenModels,
        incomingCompilerMetadata = metadata.map { it.compilerMetadata },
        schema = commonMetadata.schema,
        codegenModels = codegenModels,
        addTypename = addTypename.getOrElse(defaultAddTypename),
        schemaPackageName = commonMetadata.schemaPackageName,
        useSchemaPackageNameForFragments = useSchemaPackageNameForFragments.getOrElse(defaultUseSchemaPackageNameForFragments),
        scalarMapping = commonMetadata.scalarMapping,
        targetLanguage = targetLanguage,
        generateTestBuilders = generateTestBuilders.getOrElse(defaultGenerateTestBuilders),
        generateDataBuilders = generateDataBuilders.getOrElse(defaultGenerateDataBuilders),
        sealedClassesForEnumsMatching = sealedClassesForEnumsMatching.getOrElse(defaultSealedClassesForEnumsMatching),
        classesForEnumsMatching = classesForEnumsMatching.getOrElse(defaultClassesForEnumsMatching),
        generateOptionalOperationVariables = generateOptionalOperationVariables.getOrElse(defaultGenerateOptionalOperationVariables),
        addJvmOverloads = addJvmOverloads.getOrElse(defaultAddJvmOverloads),
        requiresOptInAnnotation = requiresOptInAnnotation.getOrElse(defaultRequiresOptInAnnotation),
        fieldsOnDisjointTypesMustMerge = fieldsOnDisjointTypesMustMerge.getOrElse(defaultFieldsOnDisjointTypesMustMerge),
        generatePrimitiveTypes = fieldsOnDisjointTypesMustMerge.getOrElse(defaultGeneratePrimitiveTypes),
        nullableFieldStyle = nullableFieldStyle.getOrElse(defaultNullableFieldStyle)
    )

    val outputCompilerMetadata = ApolloCompiler.write(options)

    val metadataOutputFile = metadataOutputFile.asFile.orNull
    if (metadataOutputFile != null) {
      ApolloMetadata(
          commonMetadata = if (rememberCommonMetadata) commonMetadata else null,
          compilerMetadata = outputCompilerMetadata,
          moduleName = projectName.get()
      ).writeTo(metadataOutputFile)
    }
  }

  private fun scalarMapping(): Map<String, ScalarInfo> {
    return scalarTypeMapping.getOrElse(emptyMap()).mapValues { (graphQLName, targetName) ->
      val adapterInitializerExpression = scalarAdapterMapping.getOrElse(emptyMap())[graphQLName]
      ScalarInfo(targetName, if (adapterInitializerExpression == null) RuntimeAdapterInitializer else ExpressionAdapterInitializer(adapterInitializerExpression))
    }
  }
}
