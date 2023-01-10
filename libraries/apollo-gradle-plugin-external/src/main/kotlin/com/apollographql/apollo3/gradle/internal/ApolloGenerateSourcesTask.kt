package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.ApolloMetadata
import com.apollographql.apollo3.compiler.CommonCodegenOptions
import com.apollographql.apollo3.compiler.CommonMetadata
import com.apollographql.apollo3.compiler.CompilerMetadata
import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.IncomingOptions.Companion.resolveSchema
import com.apollographql.apollo3.compiler.IrOptions
import com.apollographql.apollo3.compiler.JavaCodegenOptions
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.KotlinCodegenOptions
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.defaultAddJvmOverloads
import com.apollographql.apollo3.compiler.defaultAddTypename
import com.apollographql.apollo3.compiler.defaultAlwaysGenerateTypesMatching
import com.apollographql.apollo3.compiler.defaultClassesForEnumsMatching
import com.apollographql.apollo3.compiler.defaultCodegenModels
import com.apollographql.apollo3.compiler.defaultDecapitalizeFields
import com.apollographql.apollo3.compiler.defaultFailOnWarnings
import com.apollographql.apollo3.compiler.defaultFieldsOnDisjointTypesMustMerge
import com.apollographql.apollo3.compiler.defaultGenerateDataBuilders
import com.apollographql.apollo3.compiler.defaultGenerateFilterNotNull
import com.apollographql.apollo3.compiler.defaultGenerateFragmentImplementations
import com.apollographql.apollo3.compiler.defaultGenerateModelBuilders
import com.apollographql.apollo3.compiler.defaultGenerateOptionalOperationVariables
import com.apollographql.apollo3.compiler.defaultGeneratePrimitiveTypes
import com.apollographql.apollo3.compiler.defaultGenerateQueryDocument
import com.apollographql.apollo3.compiler.defaultGenerateResponseFields
import com.apollographql.apollo3.compiler.defaultGenerateSchema
import com.apollographql.apollo3.compiler.defaultGeneratedSchemaName
import com.apollographql.apollo3.compiler.defaultNullableFieldStyle
import com.apollographql.apollo3.compiler.defaultRequiresOptInAnnotation
import com.apollographql.apollo3.compiler.defaultSealedClassesForEnumsMatching
import com.apollographql.apollo3.compiler.defaultUseSemanticNaming
import com.apollographql.apollo3.compiler.defaultWarnOnDeprecatedUsages
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.ir.IrSchemaBuilder
import com.apollographql.apollo3.compiler.toUsedCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
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

@Suppress("UnstableApiUsage") // Because the gradle-api we link against has a lot of symbols still experimental
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
  abstract val usedCoordinates: ConfigurableFileCollection

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
  abstract val generateModelBuilders: Property<Boolean>

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
  abstract val generateDataBuilders: Property<Boolean>

  @get:Input
  abstract val projectPath: Property<String>

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

  @get:Input
  @get:Optional
  abstract val decapitalizeFields: Property<Boolean>

  @get:Internal
  lateinit var compilerKotlinHooks: ApolloCompilerKotlinHooks

  @Input
  fun getCompilerKotlinHooksVersion() = compilerKotlinHooks.version

  @get:Internal
  lateinit var compilerJavaHooks: ApolloCompilerJavaHooks

  @Input
  fun getCompilerJavaHooksVersion() = compilerJavaHooks.version

  @TaskAction
  fun taskAction() {
    val metadata = metadataFiles.files.toList().map { ApolloMetadata.readFrom(it) }

    val commonMetadatas = metadata.mapNotNull { it.commonMetadata }

    check(commonMetadatas.size <= 1) {
      "Apollo: multiple schemas found in metadata"
    }

    var commonMetadata = commonMetadatas.singleOrNull()
    var rememberCommonMetadata = false
    val generateDataBuilders = generateDataBuilders.getOrElse(defaultGenerateDataBuilders)
    val moduleName = projectPath.get()

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
      if (generateDataBuilders) {
        metadata.forEach {
          check(it.generateDataBuilders) {
            "Apollo: set `generateDataBuilders.set(true)` in upstream module '${it.moduleName}' in order to use data builders from current module '$moduleName'"
          }
        }
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
    val flattenModels = when (targetLanguage) {
      TargetLanguage.JAVA -> {
        check(flattenModels.isPresent.not()) {
          "Java codegen does not support flattenModels"
        }
        true
      }

      else -> {
        // Operation-based models have few name clashes. Mostly when there are lists. For these few cases we flatten to avoid the name clash
        // Response-based models would have way too much name clashes os we never flatten them
        flattenModels.getOrElse(commonMetadata.codegenModels != MODELS_RESPONSE_BASED)
      }
    }
    val codegenModels = when (targetLanguage) {
      TargetLanguage.JAVA -> {
        check(commonMetadata.codegenModels == MODELS_OPERATION_BASED) {
          "Java codegen does not support codegenModels=${commonMetadata.codegenModels}"
        }
        MODELS_OPERATION_BASED
      }

      else -> commonMetadata.codegenModels
    }

    val alwaysGenerateTypesMatching = usedCoordinates.files.map { it.toUsedCoordinates() }
        .fold(alwaysGenerateTypesMatching.getOrElse(defaultAlwaysGenerateTypesMatching)) { acc, new ->
          acc + new
        } + commonMetadata.scalarMapping.keys + setOf("String", "Boolean", "Int", "Float", "ID")

    val irOptions = IrOptions(
        schema = commonMetadata.schema,
        executableFiles = graphqlFiles.files,
        warnOnDeprecatedUsages = warnOnDeprecatedUsages.getOrElse(defaultWarnOnDeprecatedUsages),
        failOnWarnings = failOnWarnings.getOrElse(defaultFailOnWarnings),
        logger = logger,
        flattenModels = flattenModels,
        incomingFragments = metadata.flatMap { it.compilerMetadata.fragments },
        codegenModels = codegenModels,
        addTypename = addTypename.getOrElse(defaultAddTypename),
        decapitalizeFields = decapitalizeFields.getOrElse(defaultDecapitalizeFields),
        fieldsOnDisjointTypesMustMerge = fieldsOnDisjointTypesMustMerge.getOrElse(defaultFieldsOnDisjointTypesMustMerge),
        generateOptionalOperationVariables = generateOptionalOperationVariables.getOrElse(defaultGenerateOptionalOperationVariables),
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
        generateDataBuilders = generateDataBuilders,
        )

    val irOperations = ApolloCompiler.buildIrOperations(irOptions)

    val operationOutput = ApolloCompiler.buildOperationOutput(
        ir = irOperations,
        operationOutputFile = operationOutputFile.asFile.orNull,
        operationOutputGenerator = operationOutputGenerator,
    )

    val irSchema = IrSchemaBuilder.build(
        schema = commonMetadata.schema,
        irOperations = irOperations,
        incomingTypes = metadata.flatMap { it.compilerMetadata.resolverInfo.entries.filter { it.key.kind == ResolverKeyKind.SchemaType }.map { it.key.id } }.toSet(),
    )

    val commonCodegenOptions = CommonCodegenOptions(
        schema = commonMetadata.schema,
        ir = irOperations,
        irSchema = irSchema,
        operationOutput= operationOutput,
        outputDir = outputDir.asFile.get(),
        useSemanticNaming = useSemanticNaming.getOrElse(defaultUseSemanticNaming),
        packageNameGenerator = packageNameGenerator,
        generateFragmentImplementations = generateFragmentImplementations.getOrElse(defaultGenerateFragmentImplementations),
        generateQueryDocument = generateQueryDocument.getOrElse(defaultGenerateQueryDocument),
        generateSchema = generateSchema.getOrElse(defaultGenerateSchema),
        generatedSchemaName = generatedSchemaName.getOrElse(defaultGeneratedSchemaName),
        generateResponseFields = generateResponseFields.getOrElse(defaultGenerateResponseFields),
        schemaPackageName = commonMetadata.schemaPackageName,
        scalarMapping = commonMetadata.scalarMapping,
        incomingResolverInfos = metadata.map { it.compilerMetadata.resolverInfo },
    )

    val resolverInfo = when (targetLanguage) {
      TargetLanguage.JAVA -> {
        val javaCodegenOptions = JavaCodegenOptions(
            nullableFieldStyle = nullableFieldStyle.getOrElse(defaultNullableFieldStyle),
            compilerJavaHooks = compilerJavaHooks,
            generateModelBuilders = generateModelBuilders.getOrElse(defaultGenerateModelBuilders),
            classesForEnumsMatching = classesForEnumsMatching.getOrElse(defaultClassesForEnumsMatching),
            generatePrimitiveTypes = fieldsOnDisjointTypesMustMerge.getOrElse(defaultGeneratePrimitiveTypes),

            )
        ApolloCompiler.writeJava(
            commonCodegenOptions = commonCodegenOptions,
            javaCodegenOptions = javaCodegenOptions
        )
      }

      else -> {
        val kotlinCodegenOptions = KotlinCodegenOptions(
            generateAsInternal = false,
            generateFilterNotNull = generateFilterNotNull.getOrElse(defaultGenerateFilterNotNull),
            sealedClassesForEnumsMatching = sealedClassesForEnumsMatching.getOrElse(defaultSealedClassesForEnumsMatching),
            addJvmOverloads = addJvmOverloads.getOrElse(defaultAddJvmOverloads),
            requiresOptInAnnotation = requiresOptInAnnotation.getOrElse(defaultRequiresOptInAnnotation),
            compilerKotlinHooks = compilerKotlinHooks,
            languageVersion = targetLanguage
        )
        ApolloCompiler.writeKotlin(
            commonCodegenOptions = commonCodegenOptions,
            kotlinCodegenOptions = kotlinCodegenOptions
        )
      }
    }

    val outputCompilerMetadata = CompilerMetadata(
        fragments = irOperations.fragmentDefinitions,
        resolverInfo = resolverInfo,
    )

    val metadataOutputFile = metadataOutputFile.asFile.orNull
    if (metadataOutputFile != null) {
      ApolloMetadata(
          commonMetadata = if (rememberCommonMetadata) commonMetadata else null,
          compilerMetadata = outputCompilerMetadata,
          moduleName = projectPath.get(),
          generateDataBuilders = generateDataBuilders,
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
