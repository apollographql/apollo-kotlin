package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.compiler.introspection.toGQLDocument
import java.io.File


const val MODELS_RESPONSE_BASED = "responseBased"
const val MODELS_OPERATION_BASED = "operationBased"

@Deprecated(
    "MODELS_COMPAT is provided for 2.x compatibility and will be removed in a future version.",
    replaceWith = ReplaceWith("MODELS_OPERATION_BASED")
)
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
     * The directory where to write the generated models
     */
    val outputDir: File,
    /**
     * The directory where to write the generated models test code
     */
    val testDir: File,
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
    /**
     * Currently only used when [targetLanguage] is "kotlin".
     */
    val targetLanguageVersion: String = defaultTargetLanguageVersion,

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

    /**
     * Whether to generate the __Schema class.
     *
     * __Schema is a special class that contains a list of all composite types (objects, interfaces, unions)
     * It can be used to retrieve the list of possible types for a given CompiledType
     */
    val generateSchema: Boolean = defaultGenerateSchema,
    /**
     * Whether to generate the type safe Data builders. These are mainly used for tests but can also be used for other use
     * cases too.
     *
     * Only valid when [targetLanguage] is "kotlin"
     */
    val generateTestBuilders: Boolean = defaultGenerateTestBuilders,
    val moduleName: String = defaultModuleName,

    /**
     * A list of [Regex] patterns for GraphQL enums that should be generated as Kotlin sealed classes instead of the default Kotlin enums.
     *
     * Use this if you want your client to have access to the rawValue of the enum. This can be useful if new GraphQL enums are added but
     * the client was compiled against an older schema that doesn't have knowledge of the new enums.
     *
     * Default: listOf(".*")
     */
    @Deprecated("Kotlin sealed classes are more flexible than Kotlin enums to represent GraphQL enums because they can expose the" +
        "rawValue of the unknown enums.")
    val sealedClassesForEnumsMatching: List<String> = defaultSealedClassesForEnumsMatching,

    /**
     * Whether to generate operation variables as [com.apollographql.apollo3.api.Optional]
     *
     * Using [com.apollographql.apollo3.api.Optional] allows to omit the variables if needed but makes the
     * callsite more verbose in most cases.
     *
     * Default: false
     */
    val generateOptionalOperationVariables: Boolean = defaultGenerateOptionalOperationVariables
) {

  /**
   * A shorthand version that takes a File as input for the schema as well as a simple packageName and
   * has default values for quick configuration.
   * XXX: move this to a builder?
   */
  constructor(
      executableFiles: Set<File>,
      schemaFile: File,
      outputDir: File,
      testDir: File = outputDir,
      packageName: String = "",
  ) : this(
      executableFiles = executableFiles,
      schema = schemaFile.toGQLDocument().toSchema(),
      outputDir = outputDir,
      testDir = testDir,
      schemaPackageName = packageName,
      packageNameGenerator = PackageNameGenerator.Flat(packageName),
  )

  fun copy(
      schema: Schema = this.schema,
      outputDir: File = this.outputDir,
      testDir: File = this.testDir,
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
      generateSchema: Boolean = this.generateSchema,
      moduleName: String = this.moduleName,
      targetLanguage: String = this.targetLanguage,
      targetLanguageVersion: String = this.targetLanguageVersion,
      generateTestBuilders: Boolean = this.generateTestBuilders,
      sealedClassesForEnumsMatching: List<String> = this.sealedClassesForEnumsMatching,
      generateOptionalOperationVariables: Boolean = this.generateOptionalOperationVariables
  ) = Options(
      executableFiles = executableFiles,
      schema = schema,
      outputDir = outputDir,
      debugDir = debugDir,
      operationOutputFile = operationOutputFile,
      schemaPackageName = schemaPackageName,
      packageNameGenerator = packageNameGenerator,
      alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
      operationOutputGenerator = operationOutputGenerator,
      incomingCompilerMetadata = incomingCompilerMetadata,
      targetLanguage = targetLanguage,
      targetLanguageVersion = targetLanguageVersion,
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
      generateSchema = generateSchema,
      moduleName = moduleName,
      generateTestBuilders = generateTestBuilders,
      testDir = testDir,
      sealedClassesForEnumsMatching =  sealedClassesForEnumsMatching,
      generateOptionalOperationVariables = generateOptionalOperationVariables,
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
    const val defaultTargetLanguageVersion = ""
    const val defaultGenerateSchema = false
    const val defaultGenerateTestBuilders = false
    val defaultSealedClassesForEnumsMatching = listOf(".*")
    const val defaultGenerateOptionalOperationVariables = false
  }
}

