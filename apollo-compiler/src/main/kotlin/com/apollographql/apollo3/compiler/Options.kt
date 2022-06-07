package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_3_1
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.introspection.toGQLDocument
import com.apollographql.apollo3.ast.introspection.toSchema
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import java.io.File


const val MODELS_RESPONSE_BASED = "responseBased"
const val MODELS_OPERATION_BASED = "operationBased"

@Deprecated(
    "MODELS_COMPAT is provided for 2.x compatibility and will be removed in a future version.",
    replaceWith = ReplaceWith("MODELS_OPERATION_BASED")
)
@ApolloDeprecatedSince(v3_0_0)
const val MODELS_COMPAT = "compat"

const val ADD_TYPENAME_IF_FRAGMENTS = "ifFragments"
const val ADD_TYPENAME_IF_POLYMORPHIC = "ifPolymorphic"
const val ADD_TYPENAME_IF_ABSTRACT = "ifAbstract"
const val ADD_TYPENAME_ALWAYS = "always"

enum class TargetLanguage {
  // The order is important. See [isTargetLanguageVersionAtLeast]
  JAVA,

  @Deprecated("Use KOTLIN_1_5", replaceWith = ReplaceWith("KOTLIN_1_5"))
  @ApolloDeprecatedSince(v3_3_1)
  KOTLIN_1_4,

  KOTLIN_1_5,
}

@ApolloExperimental
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
     * Whether to use the schemaPackageName for fragments
     */
    val useSchemaPackageNameForFragments: Boolean = defaultUseSchemaPackageNameForFragments,
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
    val targetLanguage: TargetLanguage = defaultTargetLanguage,

    //========== codegen options ============
    val scalarMapping: Map<String, ScalarInfo> = defaultScalarMapping,
    val codegenModels: String = defaultCodegenModels,
    val flattenModels: Boolean = defaultFlattenModels,
    val useSemanticNaming: Boolean = defaultUseSemanticNaming,
    val warnOnDeprecatedUsages: Boolean = defaultWarnOnDeprecatedUsages,
    val failOnWarnings: Boolean = defaultFailOnWarnings,
    val logger: ApolloCompiler.Logger = defaultLogger,
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
     * Whether to generate the Schema class.
     *
     * The Schema class is a special class that contains a list of all composite types (objects, interfaces, unions).
     * It can be used to retrieve the list of possible types for a given CompiledType.
     *
     * Its name can be configured with [generatedSchemaName].
     *
     * Default: false
     */
    val generateSchema: Boolean = defaultGenerateSchema,

    /**
     * Class name to use when generating the Schema class.
     *
     * Default: "__Schema"
     */
    val generatedSchemaName: String = defaultGeneratedSchemaName,

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
     * Default: emptyList()
     */
    val sealedClassesForEnumsMatching: List<String> = defaultSealedClassesForEnumsMatching,

    /**
     * Whether to generate operation variables as [com.apollographql.apollo3.api.Optional]
     *
     * Using [com.apollographql.apollo3.api.Optional] allows to omit the variables if needed but makes the
     * callsite more verbose in most cases.
     *
     * Default: true
     */
    val generateOptionalOperationVariables: Boolean = defaultGenerateOptionalOperationVariables,

    /**
     * Whether to generate kotlin constructors with `@JvmOverloads` for more graceful Java interop experience when default values are present.
     * Note: when enabled in a multi-platform setup, the generated code can only be used in the common or JVM sourcesets.
     *
     * Default: false
     */
    val addJvmOverloads: Boolean = false,
    val addTypename: String = defaultAddTypename,
    val requiresOptInAnnotation: String? = defaultRequiresOptInAnnotation
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
      schema = schemaFile.toSchema(),
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
      useSchemaPackageNameForFragments: Boolean = this.useSchemaPackageNameForFragments,
      packageNameGenerator: PackageNameGenerator = this.packageNameGenerator,
      alwaysGenerateTypesMatching: Set<String> = this.alwaysGenerateTypesMatching,
      operationOutputGenerator: OperationOutputGenerator = this.operationOutputGenerator,
      incomingCompilerMetadata: List<CompilerMetadata> = this.incomingCompilerMetadata,
      scalarMapping: Map<String, ScalarInfo> = this.scalarMapping,
      codegenModels: String = this.codegenModels,
      flattenModels: Boolean = this.flattenModels,
      useSemanticNaming: Boolean = this.useSemanticNaming,
      warnOnDeprecatedUsages: Boolean = this.warnOnDeprecatedUsages,
      failOnWarnings: Boolean = this.failOnWarnings,
      logger: ApolloCompiler.Logger = this.logger,
      generateAsInternal: Boolean = this.generateAsInternal,
      generateFilterNotNull: Boolean = this.generateFilterNotNull,
      generateFragmentImplementations: Boolean = this.generateFragmentImplementations,
      generateResponseFields: Boolean = this.generateResponseFields,
      generateQueryDocument: Boolean = this.generateQueryDocument,
      generateSchema: Boolean = this.generateSchema,
      generatedSchemaName: String = this.generatedSchemaName,
      moduleName: String = this.moduleName,
      targetLanguage: TargetLanguage = this.targetLanguage,
      generateTestBuilders: Boolean = this.generateTestBuilders,
      sealedClassesForEnumsMatching: List<String> = this.sealedClassesForEnumsMatching,
      generateOptionalOperationVariables: Boolean = this.generateOptionalOperationVariables,
      addJvmOverloads: Boolean = this.addJvmOverloads,
      addTypename: String = this.addTypename,
      requiresOptInAnnotation: String? = this.requiresOptInAnnotation
  ) = Options(
      executableFiles = executableFiles,
      schema = schema,
      outputDir = outputDir,
      debugDir = debugDir,
      operationOutputFile = operationOutputFile,
      schemaPackageName = schemaPackageName,
      useSchemaPackageNameForFragments = useSchemaPackageNameForFragments,
      packageNameGenerator = packageNameGenerator,
      alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
      operationOutputGenerator = operationOutputGenerator,
      incomingCompilerMetadata = incomingCompilerMetadata,
      targetLanguage = targetLanguage,
      scalarMapping = scalarMapping,
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
      generatedSchemaName = generatedSchemaName,
      moduleName = moduleName,
      generateTestBuilders = generateTestBuilders,
      testDir = testDir,
      sealedClassesForEnumsMatching = sealedClassesForEnumsMatching,
      generateOptionalOperationVariables = generateOptionalOperationVariables,
      addJvmOverloads = addJvmOverloads,
      addTypename = addTypename,
      requiresOptInAnnotation = requiresOptInAnnotation,
  )

  companion object {
    val defaultAlwaysGenerateTypesMatching = emptySet<String>()
    val defaultOperationOutputGenerator = OperationOutputGenerator.Default(OperationIdGenerator.Sha256)
    val defaultScalarMapping = emptyMap<String, ScalarInfo>()
    val defaultLogger = ApolloCompiler.NoOpLogger
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
    const val defaultAddTypename = ADD_TYPENAME_IF_FRAGMENTS
    const val defaultRequiresOptInAnnotation = "none"
    const val defaultFlattenModels = true
    val defaultTargetLanguage = TargetLanguage.KOTLIN_1_5
    const val defaultGenerateSchema = false
    const val defaultGeneratedSchemaName = "__Schema"
    const val defaultGenerateTestBuilders = false
    val defaultSealedClassesForEnumsMatching = emptyList<String>()
    const val defaultGenerateOptionalOperationVariables = true
    const val defaultUseSchemaPackageNameForFragments = false
    const val defaultAddJvmOverloads = false
  }
}

/**
 * Controls how scalar adapters are used in the generated code.
 */
@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed interface AdapterInitializer

/**
 * The adapter expression will be used as-is (can be an object, a public val, a class instantiation).
 *
 * e.g. `"com.example.MyAdapter"` or `"com.example.MyAdapter()"`.
 */
@TypeLabel("Expression")
@JsonClass(generateAdapter = true)
class ExpressionAdapterInitializer(val expression: String) : AdapterInitializer

/**
 * The adapter instance will be looked up in the [com.apollographql.apollo3.api.CustomScalarAdapters] provided at runtime.
 */
@TypeLabel("Runtime")
object RuntimeAdapterInitializer : AdapterInitializer

@JsonClass(generateAdapter = true)
data class ScalarInfo(val targetName: String, val adapterInitializer: AdapterInitializer = RuntimeAdapterInitializer)
