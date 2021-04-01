package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.frontend.Schema
import java.io.File

/**
 * These are the options that should be the same in all modules.
 */
class RootOptions(
    val schema: Schema,
    val schemaPackageName: String,
    val customScalarsMapping: Map<String, String>,
    val generateFragmentsAsInterfaces: Boolean,
    val metadataInputObjects: Set<String>,
    val metadataEnums: Set<String>,
    val metadataCustomScalars: Boolean,
    val metadataFragments: List<MetadataFragment>,
) {
  companion object {
    fun fromMetadata(metadata: ApolloMetadata): RootOptions {
      return RootOptions(
          schema = metadata.schema!!,
          schemaPackageName = metadata.schemaPackageName,
          customScalarsMapping = metadata.customScalarsMapping,
          generateFragmentsAsInterfaces = metadata.generateFragmentsAsInterfaces,
          metadataInputObjects = metadata.generatedInputObjects,
          metadataEnums = metadata.generatedEnums,
          metadataCustomScalars = true,
          metadataFragments = metadata.generatedFragments
      )
    }

    fun from(
        roots: Roots,
        schemaFile: File,
        customScalarsMapping: Map<String, String>,
        generateFragmentsAsInterfaces: Boolean,
        rootPackageName: String,
    ): RootOptions {
      val relativeSchemaPackageName = try {
        roots.filePackageName(schemaFile.absolutePath)
      } catch (e: Exception) {
        ""
      }
      return RootOptions(
          schema = Schema.fromFile(schemaFile),
          schemaPackageName = "$rootPackageName.$relativeSchemaPackageName".removePrefix(".").removeSuffix("."),
          customScalarsMapping = customScalarsMapping,
          generateFragmentsAsInterfaces = generateFragmentsAsInterfaces,
          metadataInputObjects = emptySet(),
          metadataEnums = emptySet(),
          metadataCustomScalars = false,
          metadataFragments = emptyList()
      )
    }
  }
}

/**
 * A version of the GraphQL compiler that writes metadata
 * It's a lot of copy/pasta but ultimately I didn't find a way to express in an easy way that some options might come from
 * the metadata.
 */
fun GraphQLCompiler.Companion.writeWithMetadata(
    rootOptions: RootOptions,
    operationFiles: Set<File>,
    outputDir: File,
    packageNameProvider: PackageNameProvider,
    alwaysGenerateTypesMatching: Set<String>,
    operationOutputFile: File?,
    operationOutputGenerator: OperationOutputGenerator,
    useSemanticNaming: Boolean,
    warnOnDeprecatedUsages: Boolean,
    failOnWarnings: Boolean,
    logger: GraphQLCompiler.Logger,
    generateAsInternal: Boolean,
    generateFilterNotNull: Boolean,
    enumAsSealedClassPatternFilters: Set<String>,
    generateFragmentImplementations: Boolean,
    generateResponseFields: Boolean,
    generateQueryDocument: Boolean,
    useUnifiedIr: Boolean,
    metadataOutputFile: File?,
    moduleName: String,
) {
  val args = GraphQLCompiler.Arguments(
      operationFiles = operationFiles,
      schema = rootOptions.schema,
      outputDir = outputDir,

      metadataFragments = rootOptions.metadataFragments,
      metadataInputObjects = rootOptions.metadataInputObjects,
      metadataEnums = rootOptions.metadataEnums,
      metadataCustomScalars = rootOptions.metadataCustomScalars,
      customScalarsMapping = rootOptions.customScalarsMapping,
      generateFragmentsAsInterfaces = rootOptions.generateFragmentsAsInterfaces,

      packageNameProvider = packageNameProvider,
      alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
      operationOutputFile = operationOutputFile,
      operationOutputGenerator = operationOutputGenerator,
      useSemanticNaming = useSemanticNaming,
      warnOnDeprecatedUsages = warnOnDeprecatedUsages,
      failOnWarnings = failOnWarnings,
      logger = logger,
      generateAsInternal = generateAsInternal,
      generateFilterNotNull = generateFilterNotNull,
      enumAsSealedClassPatternFilters = enumAsSealedClassPatternFilters,
      generateFragmentImplementations = generateFragmentImplementations,
      generateResponseFields = generateResponseFields,
      generateQueryDocument = generateQueryDocument,
      useUnifiedIr = useUnifiedIr,
  )

  val result = GraphQLCompiler().write(args)

  if (metadataOutputFile != null) {
    check(!generateAsInternal) {
      "Specifying 'generateAsInternal=true' does not make sense in a multi-module setup"
    }
    ApolloMetadata(
        schema = rootOptions.schema,
        customScalarsMapping = rootOptions.customScalarsMapping,
        generatedFragments = result.generatedFragments,
        generatedEnums = result.generatedEnums,
        generatedInputObjects = result.generatedInputObjects,
        generateFragmentsAsInterfaces = rootOptions.generateFragmentsAsInterfaces,
        moduleName = moduleName,
        pluginVersion = VERSION,
        schemaPackageName = rootOptions.schemaPackageName
    ).writeTo(metadataOutputFile)
  }
}

