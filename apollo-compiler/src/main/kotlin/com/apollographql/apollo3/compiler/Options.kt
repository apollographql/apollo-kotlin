package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.compiler.introspection.toGQLDocument
import java.io.File


const val MODELS_RESPONSE_BASED = "responseBased"
const val MODELS_OPERATION_BASED = "operationBased"
const val MODELS_COMPAT = "compat"

const val TARGET_KOTLIN = "kotlin"
const val TARGET_JAVA = "java"

class Options(
    /**
     * The files containing the operations and fragments
     */
    val executableFiles: Set<File>,
    /**
     * The schema as obtained by [toGQLDocument].[toSchema]
     *
     * In order to pass a single schema file, see the secondary constructors
     */
    val schema: Schema,
    /**
     * The output directory where to write the generated models
     */
    val outputDir: File,
    /**
     * A debug directory to dump some intermediate artifacts.
     */
    val debugDir: File? = null,
    /**
     * the file where to write the operationOutput or null if no operationOutput is required
     * OperationOutput represents the modified operations as they are sent to the server. This is useful for whitelisting/
     * persisted queries
     */
    val operationOutputFile: File? = null,
    /**
     * The package name used as a base for input objects, fragments, enums and types
     */
    val schemaPackageName: String,
    /**
     * The package name used for operations
     */
    val packageNameGenerator: PackageNameGenerator,
    /**
     * Additional enum/input types to generate.
     * For input types, this will recursively add all input fields types/enums.
     */
    val alwaysGenerateTypesMatching: Set<String> = defaultAlwaysGenerateTypesMatching,
    /**
     * the OperationOutputGenerator used to generate operation Ids
     */
    val operationOutputGenerator: OperationOutputGenerator = defaultOperationOutputGenerator,
    /**
     * The metadata from upstream
     */
    val incomingCompilerMetadata: List<CompilerMetadata> = emptyList(),
    val targetLanguage: String = defaultTargetLanguage,

    //========== codegen options ============
    val customScalarsMapping: Map<String, String> = defaultCustomScalarsMapping,
    val codegenModels: String = defaultCodegenModels,
    val flattenModels: Boolean = defaultFlattenModels,
    val useSemanticNaming: Boolean = defaultUseSemanticNaming,
    val warnOnDeprecatedUsages: Boolean = defaultWarnOnDeprecatedUsages,
    val failOnWarnings: Boolean = defaultFailOnWarnings,
    val logger: GraphQLCompiler.Logger = defaultLogger,
    val generateAsInternal: Boolean = defaultGenerateAsInternal,
    /**
     * Kotlin native will generate [Any?] for optional types
     * Setting generateFilterNotNull will generate extra `filterNotNull` functions that will help keep the type information
     */
    val generateFilterNotNull: Boolean = defaultGenerateFilterNotNull,

    //========== on/off flags to switch some codegen off ============

    /**
     * Whether to generate the [com.apollographql.apollo3.api.Fragment] as well as response and variables adapters.
     * If generateFragmentsAsInterfaces is true, this will also generate data classes for the fragments.
     *
     * Set to true if you need to read/write fragments from the cache or if you need to instantiate fragments
     */
    val generateFragmentImplementations: Boolean = defaultGenerateFragmentImplementations,
    /**
     * Whether to generate the compiled selections used to read/write from the normalized cache.
     * Disable this option if you don't use the normalized cache to save some bytecode
     */
    val generateResponseFields: Boolean = defaultGenerateResponseFields,
    /**
     * Whether to embed the query document in the [com.apollographql.apollo3.api.Operation]s. By default this is true as it is needed
     * to send the operations to the server.
     * If performance is critical and you have a way to whitelist/read the document from another place, disable this.
     */
    val generateQueryDocument: Boolean = defaultGenerateQueryDocument,
    val moduleName: String,
) {

  /**
   * A shorthand version that takes a File as input for the schema as well as a simple packageName and
   * has default values for quick configuration
   */
  constructor(
      executableFiles: Set<File>,
      schemaFile: File,
      outputDir: File,
      debugDir: File? = null,
      operationOutputFile: File? = null,
      packageName: String = "",
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
      targetLanguage: String = defaultTargetLanguage,
  ) : this(
      executableFiles = executableFiles,
      schema = schemaFile.toGQLDocument().toSchema(),
      outputDir = outputDir,
      debugDir = debugDir,
      operationOutputFile = operationOutputFile,
      schemaPackageName = packageName,
      packageNameGenerator = PackageNameGenerator.Flat(packageName),
      incomingCompilerMetadata = emptyList(),
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
      targetLanguage = targetLanguage
  )

  fun copy(
      schema: Schema = this.schema,
      outputDir: File = this.outputDir,
      debugDir: File? = this.debugDir,
      operationOutputFile: File? = this.operationOutputFile,
      executableFiles: Set<File> = this.executableFiles,
      schemaPackageName: String = this.schemaPackageName,
      packageNameGenerator: PackageNameGenerator = this.packageNameGenerator,
      alwaysGenerateTypesMatching: Set<String> = this.alwaysGenerateTypesMatching,
      operationOutputGenerator: OperationOutputGenerator = this.operationOutputGenerator,
      incomingCompilerMetadata: List<CompilerMetadata> = this.incomingCompilerMetadata,
      customScalarsMapping: Map<String, String> = this.customScalarsMapping,
      codegenModels: String = this.codegenModels,
      flattenModels: Boolean = this.flattenModels,
      useSemanticNaming: Boolean = this.useSemanticNaming,
      warnOnDeprecatedUsages: Boolean = this.warnOnDeprecatedUsages,
      failOnWarnings: Boolean = this.failOnWarnings,
      logger: GraphQLCompiler.Logger = this.logger,
      generateAsInternal: Boolean = this.generateAsInternal,
      generateFilterNotNull: Boolean = this.generateFilterNotNull,
      generateFragmentImplementations: Boolean = this.generateFragmentImplementations,
      generateResponseFields: Boolean = this.generateResponseFields,
      generateQueryDocument: Boolean = this.generateQueryDocument,
      moduleName: String = this.moduleName,
      targetLanguage: String = this.targetLanguage,
  ) = Options(
      schema = schema,
      outputDir = outputDir,
      debugDir = debugDir,
      executableFiles = executableFiles,
      operationOutputFile = operationOutputFile,
      schemaPackageName = schemaPackageName,
      packageNameGenerator = packageNameGenerator,
      alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
      operationOutputGenerator = operationOutputGenerator,
      incomingCompilerMetadata = incomingCompilerMetadata,
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
      targetLanguage = targetLanguage
  )

  companion object {
    val defaultAlwaysGenerateTypesMatching = emptySet<String>()
    val defaultOperationOutputGenerator = OperationOutputGenerator.Default(OperationIdGenerator.Sha256)
    val defaultCustomScalarsMapping = emptyMap<String, String>()
    val defaultLogger = GraphQLCompiler.NoOpLogger
    const val defaultUseSemanticNaming = true
    const val defaultWarnOnDeprecatedUsages = true
    const val defaultFailOnWarnings = false
    const val defaultGenerateAsInternal = false
    const val defaultGenerateFilterNotNull = false
    const val defaultGenerateFragmentsAsInterfaces = false
    const val defaultGenerateFragmentImplementations = false
    const val defaultGenerateResponseFields = true
    const val defaultGenerateQueryDocument = true
    const val defaultModuleName = "apollographql"
    const val defaultCodegenModels = MODELS_OPERATION_BASED
    const val defaultFlattenModels = true
    const val defaultTargetLanguage = TARGET_KOTLIN
  }
}

