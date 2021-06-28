package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.compiler.Options.Companion.defaultCodegenModels
import com.apollographql.apollo3.compiler.Options.Companion.defaultFailOnWarnings
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateAsInternal
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateFilterNotNull
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateFragmentImplementations
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateQueryDocument
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateResponseFields
import com.apollographql.apollo3.compiler.Options.Companion.defaultLogger
import com.apollographql.apollo3.compiler.Options.Companion.defaultModuleName
import com.apollographql.apollo3.compiler.Options.Companion.defaultOperationOutputGenerator
import com.apollographql.apollo3.compiler.Options.Companion.defaultUseSemanticNaming
import com.apollographql.apollo3.compiler.Options.Companion.defaultWarnOnDeprecatedUsages
import com.apollographql.apollo3.compiler.introspection.toGQLDocument
import java.io.File


const val MODELS_RESPONSE_BASED = "responseBased"
const val MODELS_OPERATION_BASED = "operationBased"
const val MODELS_COMPAT = "compat"

class Options(
    val executableFiles: Set<File>,
    val schema: Schema,

    val outputDir: File,
    val debugDir: File? = null,
    val metadataOutputFile: File?,
    /**
     * the file where to write the operationOutput or null if no operationOutput is required
     */
    val operationOutputFile: File?,

    /**
     * The package name used as a base for input objects, fragments, enums and types
     */
    val schemaPackageName: String,
    /**
     * The package name used for operations
     */
    val packageNameGenerator: PackageNameGenerator,

    val inputObjectsToSkip: Set<String>,
    val enumsToSkip: Set<String>,
    val metadataFragments: List<MetadataFragment>,
    val generateTypes: Boolean,
    /**
     * Additional enum/input types to generate.
     * For input types, this will recursively add all input fields types/enums.
     */
    val alwaysGenerateTypesMatching: Set<String>,

    /**
     * the OperationOutputGenerator used to generate operation Ids
     */
    val operationOutputGenerator: OperationOutputGenerator,

    //========== codegen options ============
    val customScalarsMapping: Map<String, String>,
    val codegenModels: String,
    val flattenModels: Boolean,
    val useSemanticNaming: Boolean,
    val warnOnDeprecatedUsages: Boolean,
    val failOnWarnings: Boolean,
    val logger: GraphQLCompiler.Logger,
    val generateAsInternal: Boolean,
    /**
     * Kotlin native will generate [Any?] for optional types
     * Setting generateFilterNotNull will generate extra `filterNotNull` functions that will help keep the type information
     */
    val generateFilterNotNull: Boolean,

    //========== on/off flags to switch some codegen off ============

    /**
     * Whether to generate the [com.apollographql.apollo3.api.Fragment] as well as response and variables adapters.
     * If generateFragmentsAsInterfaces is true, this will also generate data classes for the fragments.
     *
     * Set to true if you need to read/write fragments from the cache or if you need to instantiate fragments
     */
    val generateFragmentImplementations: Boolean,
    /**
     * Whether to generate the compiled selections used to read/write from the normalized cache.
     * Disable this option if you don't use the normalized cache to save some bytecode
     */
    val generateResponseFields: Boolean,
    /**
     * Whether to embed the query document in the [com.apollographql.apollo3.api.Operation]s. By default this is true as it is needed
     * to send the operations to the server.
     * If performance is critical and you have a way to whitelist/read the document from another place, disable this.
     */
    val generateQueryDocument: Boolean,
    val moduleName: String,
) {


  companion object {
    const val defaultSchemaPackageName = ""
    val defaultPackageNameGenerator = PackageNameGenerator.Flat("")
    val defaultAlwaysGenerateTypesMatching = emptySet<String>()
    val defaultOperationOutputFile = null
    val defaultOperationOutputGenerator = OperationOutputGenerator.Default(OperationIdGenerator.Sha256)
    val defaultCustomScalarsMapping = emptyMap<String, String>()
    const val defaultUseSemanticNaming = true
    const val defaultWarnOnDeprecatedUsages = true
    const val defaultFailOnWarnings = false
    val defaultLogger = GraphQLCompiler.NoOpLogger
    const val defaultGenerateAsInternal = false
    const val defaultGenerateFilterNotNull = false
    const val defaultGenerateFragmentsAsInterfaces = false
    const val defaultGenerateFragmentImplementations = false
    const val defaultGenerateResponseFields = true
    const val defaultGenerateQueryDocument = true
    const val defaultModuleName = "apollographql"
    const val defaultCodegenModels = MODELS_COMPAT
    val defaultMetadataOutputFile = null
  }
}

/**
 * A shorthand version that takes a File as input for the schema as well as a simple packageName and
 * has default values for quick configuration
 */
fun Options(
    executableFiles: Set<File>,
    schemaFile: File,
    outputDir: File,
    debugDir: File? = null,
    metadataOutputFile: File? = null,
    operationOutputFile: File? = null,
    packageName: String = "",
    inputObjectsToSkip: Set<String> = emptySet(),
    enumsToSkip: Set<String> = emptySet(),
    metadataFragments: List<MetadataFragment> = emptyList(),
    generateTypes: Boolean = true,
    alwaysGenerateTypesMatching: Set<String> = emptySet(),
    operationOutputGenerator: OperationOutputGenerator = defaultOperationOutputGenerator,
    customScalarsMapping: Map<String, String> = emptyMap(),
    codegenModels: String = defaultCodegenModels,
    flattenModels: Boolean = codegenModels != MODELS_RESPONSE_BASED,
    useSemanticNaming: Boolean = defaultUseSemanticNaming,
    warnOnDeprecatedUsages: Boolean = defaultWarnOnDeprecatedUsages,
    failOnWarnings: Boolean = defaultFailOnWarnings,
    logger: GraphQLCompiler.Logger = defaultLogger,
    generateAsInternal: Boolean = defaultGenerateAsInternal,
    generateFilterNotNull: Boolean = defaultGenerateFilterNotNull,
    generateFragmentImplementations: Boolean = defaultGenerateFragmentImplementations,
    generateResponseFields: Boolean = defaultGenerateResponseFields,
    generateQueryDocument: Boolean = defaultGenerateQueryDocument,
    moduleName: String = defaultModuleName,
): Options {
  val schema = schemaFile.toGQLDocument().toSchema()
  val packageNameGenerator = PackageNameGenerator.Flat(packageName)

  return Options(
      executableFiles = executableFiles,
      schema = schema,
      outputDir = outputDir,
      debugDir = debugDir,
      metadataOutputFile = metadataOutputFile,
      operationOutputFile = operationOutputFile,
      schemaPackageName = packageName,
      packageNameGenerator = packageNameGenerator,
      inputObjectsToSkip = inputObjectsToSkip,
      enumsToSkip = enumsToSkip,
      metadataFragments = metadataFragments,
      generateTypes = generateTypes,
      alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
      operationOutputGenerator = operationOutputGenerator,
      customScalarsMapping = customScalarsMapping,
      codegenModels = codegenModels,
      flattenModels = flattenModels,
      useSemanticNaming = useSemanticNaming,
      warnOnDeprecatedUsages = warnOnDeprecatedUsages,
      failOnWarnings = failOnWarnings,
      logger = logger,
      generateAsInternal = generateAsInternal,
      generateFilterNotNull = generateFilterNotNull,
      generateFragmentImplementations = generateFragmentImplementations,
      generateResponseFields = generateResponseFields,
      generateQueryDocument = generateQueryDocument,
      moduleName = moduleName,
  )
}

/**
 * A shorthand version that takes incomingOptions as input
 */
fun Options(
    executableFiles: Set<File>,
    outputDir: File,
    incomingOptions: IncomingOptions,
    packageNameGenerator: PackageNameGenerator,
    debugDir: File? = null,
    metadataOutputFile: File? = null,
    operationOutputFile: File? = null,
    alwaysGenerateTypesMatching: Set<String> = emptySet(),
    operationOutputGenerator: OperationOutputGenerator = defaultOperationOutputGenerator,
    customScalarsMapping: Map<String, String> = emptyMap(),
    useSemanticNaming: Boolean = defaultUseSemanticNaming,
    warnOnDeprecatedUsages: Boolean = defaultWarnOnDeprecatedUsages,
    failOnWarnings: Boolean = defaultFailOnWarnings,
    logger: GraphQLCompiler.Logger = defaultLogger,
    generateAsInternal: Boolean = defaultGenerateAsInternal,
    generateFilterNotNull: Boolean = defaultGenerateFilterNotNull,
    generateFragmentImplementations: Boolean = defaultGenerateFragmentImplementations,
    generateResponseFields: Boolean = defaultGenerateResponseFields,
    generateQueryDocument: Boolean = defaultGenerateQueryDocument,
    moduleName: String = defaultModuleName,
): Options {

  return Options(
      executableFiles = executableFiles,
      outputDir = outputDir,
      debugDir = debugDir,
      metadataOutputFile = metadataOutputFile,
      operationOutputFile = operationOutputFile,
      alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
      operationOutputGenerator = operationOutputGenerator,
      useSemanticNaming = useSemanticNaming,
      warnOnDeprecatedUsages = warnOnDeprecatedUsages,
      failOnWarnings = failOnWarnings,
      logger = logger,
      generateAsInternal = generateAsInternal,
      generateFilterNotNull = generateFilterNotNull,
      generateFragmentImplementations = generateFragmentImplementations,
      generateResponseFields = generateResponseFields,
      generateQueryDocument = generateQueryDocument,
      moduleName = moduleName,
      packageNameGenerator = packageNameGenerator,
      schema = incomingOptions.schema,
      schemaPackageName = incomingOptions.schemaPackageName,
      enumsToSkip = incomingOptions.metadataEnums,
      inputObjectsToSkip = incomingOptions.metadataInputObjects,
      generateTypes = !incomingOptions.isFromMetadata,
      metadataFragments = incomingOptions.metadataFragments,
      codegenModels = incomingOptions.codegenModels,
      flattenModels = incomingOptions.flattenModels,
      customScalarsMapping = incomingOptions.customScalarsMapping
  )
}