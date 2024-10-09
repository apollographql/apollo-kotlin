package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MODELS_RESPONSE_BASED = "responseBased"
const val MODELS_OPERATION_BASED = "operationBased"
const val MODELS_OPERATION_BASED_WITH_INTERFACES = "experimental_operationBasedWithInterfaces"

const val ADD_TYPENAME_IF_FRAGMENTS = "ifFragments"
const val ADD_TYPENAME_IF_POLYMORPHIC = "ifPolymorphic"
const val ADD_TYPENAME_IF_ABSTRACT = "ifAbstract"
const val ADD_TYPENAME_ALWAYS = "always"

@Deprecated("Use $MANIFEST_PERSISTED_QUERY instead")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_1)
const val MANIFEST_OPERATION_OUTPUT = "operationOutput"
const val MANIFEST_PERSISTED_QUERY = "persistedQueryManifest"
const val MANIFEST_NONE = "none"

enum class TargetLanguage {
  // The order is important. See [isTargetLanguageVersionAtLeast]
  JAVA,

  /**
   * Base language version.
   */
  @Deprecated("Use KOTLIN_1_9 instead" , ReplaceWith("KOTLIN_1_9"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_2)
  KOTLIN_1_5,

  /**
   * Same as [KOTLIN_1_5] but uses `entries` instead of `values()` in enums.
   */
  KOTLIN_1_9,
}

enum class JavaNullable {
  /**
   * Fields will be generated with the same type and no annotations whether they are nullable or not.
   * This is the default value.
   */
  NONE,

  /**
   * Fields will be generated as Apollo's `com.apollographql.apollo.api.Optional<Type>` if nullable, or `Type` if not.
   */
  APOLLO_OPTIONAL,

  /**
   * Fields will be generated as Java's `java.util.Optional<Type>` if nullable, or `Type` if not.
   */
  JAVA_OPTIONAL,

  /**
   * Fields will be generated as Guava's `com.google.common.base.Optional<Type>` if nullable, or `Type` if not.
   */
  GUAVA_OPTIONAL,

  /**
   * Fields will be generated with JetBrain's `org.jetbrains.annotations.Nullable` annotation if nullable, or
   * `org.jetbrains.annotations.NotNull` if not.
   */
  JETBRAINS_ANNOTATIONS,

  /**
   * Fields will be generated with Android's `androidx.annotation.Nullable` annotation if nullable, or
   * `androidx.annotation.NonNull` if not.
   */
  ANDROID_ANNOTATIONS,

  /**
   * Fields will be generated with JSR 305's `javax.annotation.Nullable` annotation if nullable, or
   * `javax.annotation.Nonnull` if not.
   */
  JSR_305_ANNOTATIONS,
  ;

  companion object {
    fun fromName(name: String): JavaNullable? {
      return when (name) {
        "none" -> NONE
        "apolloOptional" -> APOLLO_OPTIONAL
        "javaOptional" -> JAVA_OPTIONAL
        "guavaOptional" -> GUAVA_OPTIONAL
        "jetbrainsAnnotations" -> JETBRAINS_ANNOTATIONS
        "androidAnnotations" -> ANDROID_ANNOTATIONS
        "jsr305Annotations" -> JSR_305_ANNOTATIONS
        else -> null
      }
    }
  }
}

enum class GeneratedMethod {
  /**
   * Generate both hash code and equals method
   *
   */
  EQUALS_HASH_CODE,

  /**
   * Generate toString method
   *
   */
  TO_STRING,

  /**
   * Generate copy method
   *
   */
  COPY,

  /**
   * Generate class as data class, which will include equals, hash code, and toString()
   *
   */
  DATA_CLASS,
  ;

  companion object {
    fun fromName(name: String): GeneratedMethod? {
      return when (name) {
        "equalsHashCode" -> EQUALS_HASH_CODE
        "toString" -> TO_STRING
        "copy" -> COPY
        "dataClass" -> DATA_CLASS
        else -> null
      }
    }
  }
}

@Serializable
class CodegenSchemaOptions(
    val scalarMapping: Map<String, ScalarInfo>,
    val generateDataBuilders: Boolean?,
)

fun buildCodegenSchemaOptions(
    scalarMapping: Map<String, ScalarInfo> = emptyMap(),
    generateDataBuilders: Boolean? = null,
) = CodegenSchemaOptions(
    scalarMapping = scalarMapping,
    generateDataBuilders = generateDataBuilders,
)

@Serializable
class IrOptions(
    /**
     * Whether fields with different shape are disallowed to be merged in disjoint types.
     *
     * Note: setting this to `false` relaxes the standard GraphQL [FieldsInSetCanMerge](https://spec.graphql.org/draft/#FieldsInSetCanMerge()) validation which may still be
     * run on the backend.
     *
     * See also [issue 4320](https://github.com/apollographql/apollo-kotlin/issues/4320)
     *
     * Default: true.
     */
    val fieldsOnDisjointTypesMustMerge: Boolean?,

    /**
     * Whether to decapitalize field names in the generated models (for instance `FooBar` -> `fooBar`).
     *
     * Default: false
     */
    val decapitalizeFields: Boolean?,

    val flattenModels: Boolean?,

    val warnOnDeprecatedUsages: Boolean?,
    val failOnWarnings: Boolean?,
    val addTypename: String?,

    /**
     * Whether to generate operation variables as [com.apollographql.apollo.api.Optional]
     *
     * Using [com.apollographql.apollo.api.Optional] allows to omit the variables if needed but makes the
     * callsite more verbose in most cases.
     *
     * Default: true
     */
    val generateOptionalOperationVariables: Boolean?,

    /**
     * Additional scalar/enum/input types to generate.
     * For input types, this will recursively add all input fields types/enums.
     */
    val alwaysGenerateTypesMatching: Set<String>?,

    val codegenModels: String?,
)

fun buildIrOptions(
    fieldsOnDisjointTypesMustMerge: Boolean? = null,
    decapitalizeFields: Boolean? = null,
    flattenModels: Boolean? = null,
    warnOnDeprecatedUsages: Boolean? = null,
    failOnWarnings: Boolean? = null,
    addTypename: String? = null,
    generateOptionalOperationVariables: Boolean? = null,
    alwaysGenerateTypesMatching: Set<String>? = null,
    codegenModels: String? = null,
): IrOptions = IrOptions(
    fieldsOnDisjointTypesMustMerge = fieldsOnDisjointTypesMustMerge,
    decapitalizeFields = decapitalizeFields,
    flattenModels = flattenModels,
    warnOnDeprecatedUsages = warnOnDeprecatedUsages,
    failOnWarnings = failOnWarnings,
    addTypename = addTypename,
    generateOptionalOperationVariables = generateOptionalOperationVariables,
    alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
    codegenModels = codegenModels,
)

interface CommonCodegenOpt {
  /**
   * The target language to use to generate the sources
   */
  val targetLanguage: TargetLanguage?

  /**
   * The package name for generated classes. Operations use the package name directly. Other
   * classes like fragments and schema types use sub-packages to avoid name clashes:
   *
   * - $packageName/SomeQuery.kt
   * - $packageName/fragment/SomeFragment.kt
   * - $packageName/type/CustomScalar.kt
   * - etc...
   */
  val packageName: String?

  /**
   * If non-null, get the package names from the normalized file paths and prepend [rootPackageName]
   */
  val rootPackageName: String?

  /**
   * Whether to decapitalize fields  (for instance `FooBar` -> `fooBar`). This is useful if your schema has fields starting with an uppercase as
   * it may create name clashes with models that use PascalCase
   *
   * Default: false
   */
  val decapitalizeFields: Boolean?
  /**
   * When true, the operation class names are suffixed with their operation type like ('Query', 'Mutation' ot 'Subscription').
   * For an example, `query getDroid { ... }` GraphQL query generates the 'GetDroidQuery' class.
   *
   * Default value: true
   */
  val useSemanticNaming: Boolean?
  /**
   * Specifies which methods will be auto generated on operations, models, fragments and input objects.
   *
   * Pass a list of the following:
   *
   * - "equalsHashCode" generates `equals` and `hashCode` methods that will compare generated class properties.
   * - "toString" generates a method that will print a pretty string representing the data in the class.
   * - "copy" (Kotlin only) generates a method that will copy the class with named parameters and default values.
   * - "dataClass" (Kotlin only and redundant with all other methods) generates the class as a [data class](https://kotlinlang.org/docs/data-classes.html)
   * which will automatically generate `toString`, `copy`, `equals` and `hashCode`.
   *
   * Default for kotlin: `listOf("dataClass")`
   * Default for Java: `listOf("equalsHashCode", "toString")`
   */
  val generateMethods: List<GeneratedMethod>?

  /**
   * The format to output for the operation manifest. Valid values are:
   *
   * - "operationOutput": a manifest that matches the format used by [OperationOutputGenerator]
   * - "persistedQueryManifest": a manifest format for an upcoming GraphOS feature
   * - nothing (Default): by default no manifest is generated
   *
   * "operationOutput" uses a JSON format like so:
   * ```json
   * {
   *   "3f8a446ab7672c1efad3735b6fa86caaeefe7ec47f87fca9b84e71e0d93e6bea": {
   *     "name": "DroidDetails",
   *     "source": "query DroidDetails { species(id: \"c3BlY2llczoy\") { id name filmConnection { edges { node { id title } } } } }"
   *   },
   *   "e772cb55495ad5becc0c804ca3de7d5a5f31f145552bc33529f025d6cb0a8ce6": {
   *     "name": "AllFilms",
   *     "source": "query AllFilms { allFilms(first: 100) { totalCount films { title releaseDate } } }"
   *   }
   * }
   * ```
   *
   * "persistedQueryManifest" uses a format compatible with an upcoming GraphQL feature like so:
   * ```json
   * {
   *   "format": "apollo-persisted-query-manifest",
   *   "version": 1,
   *   "operations": [
   *     {
   *       "id": "dc67510fb4289672bea757e862d6b00e83db5d3cbbcfb15260601b6f29bb2b8f",
   *       "body": "query UniversalQuery { __typename }",
   *       "name": "UniversalQuery",
   *       "type": "query"
   *     },
   *     {
   *       "id": "f11e4dcb28788af2e41689bb366472084aa1aa1e1ba633c3d605279cff08ed59",
   *       "body": "query FragmentedQuery { post { ...PostFragment } }  fragment PostFragment on Post { id title }",
   *       "name": "FragmentedQuery",
   *       "type": "query"
   *     },
   *     {
   *       "id": "04649073787db6f24b495d49e5e87526734335a002edbd6e06e7315e302af5ac",
   *       "body": "mutation SetNameMutation($name: String!) { setName($name) }",
   *       "name": "SetNameMutation",
   *       "type": "mutation"
   *     }
   *   ]
   * }
   * ```
   *
   */
  val operationManifestFormat: String?
}

interface SchemaCodegenOpt {
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
  val generateSchema: Boolean?
  /**
   * Class name to use when generating the Schema class.
   *
   * Default: "__Schema"
   */
  val generatedSchemaName: String?
}

interface OperationsCodegenOpt {
  /**
   * Whether to generate the [com.apollographql.apollo.api.Fragment] as well as response and variables adapters.
   *
   * When using `responseBased` codegen, [generateFragmentImplementations] also generates classes for every fragment
   * interface.
   *
   * Most of the time, fragment implementations are not needed because you can access fragments and read all
   * data from your queries.
   * Fragment implementations are needed if you want to build fragments outside an operation. For an example
   * to programmatically build a fragment that is reused in another part of your code or to read and write fragments to the cache.
   *
   * Default: false
   */
  val generateFragmentImplementations: Boolean?

  /**
   * Whether to embed the query document in the [com.apollographql.apollo.api.Operation]s. By default, this is true as it is needed
   * to send the operations to the server.
   * If performance/binary size is critical, and you are using persisted queries or a similar mechanism, disable this.
   *
   * Default: true
   */
  val generateQueryDocument: Boolean?
}

interface JavaCodegenOpt {
  /**
   * Whether to generate builders for java models
   *
   * Default value: false
   */
  val generateModelBuilders: Boolean?

  /**
   * A list of [Regex] patterns for GraphQL enums that should be generated as Java classes.
   *
   * Use this if you want your client to have access to the rawValue of the enum. This can be useful if new GraphQL enums are added but
   * the client was compiled against an older schema that doesn't have knowledge of the new enums.
   *
   * Default: listOf(".*")
   */
  val classesForEnumsMatching: List<String>?

  /**
   * Whether to generate fields as primitive types (`int`, `double`, `boolean`) instead of their boxed types (`Integer`, `Double`,
   * `Boolean`) when possible.
   *
   * Default: false
   */
  val generatePrimitiveTypes: Boolean?

  /**
   * The style to use for fields that are nullable in the Java generated code.
   *
   * Acceptable values:
   * - `none`: Fields will be generated with the same type whether they are nullable or not
   * - `apolloOptional`: Fields will be generated as Apollo's `com.apollographql.apollo.api.Optional<Type>` if nullable, or `Type` if not.
   * - `javaOptional`: Fields will be generated as Java's `java.util.Optional<Type>` if nullable, or `Type` if not.
   * - `guavaOptional`: Fields will be generated as Guava's `com.google.common.base.Optional<Type>` if nullable, or `Type` if not.
   * - `jetbrainsAnnotations`: Fields will be generated with Jetbrain's `org.jetbrains.annotations.Nullable` annotation if nullable, or
   * `org.jetbrains.annotations.NotNull` if not.
   * - `androidAnnotations`: Fields will be generated with Android's `androidx.annotation.Nullable` annotation if nullable, or
   * `androidx.annotation.NonNull` if not.
   * - `jsr305Annotations`: Fields will be generated with JSR 305's `javax.annotation.Nullable` annotation if nullable, or
   * `javax.annotation.Nonnull` if not.
   *
   * Default: `none`
   */
  val nullableFieldStyle: JavaNullable?
}

interface KotlinCodegenOpt {
  /**
   * A list of [Regex] patterns for GraphQL enums that should be generated as Kotlin sealed classes instead of the default Kotlin enums.
   *
   * Use this if you want your client to have access to the rawValue of the enum. This can be useful if new GraphQL enums are added but
   * the client was compiled against an older schema that doesn't have knowledge of the new enums.
   *
   * Default: emptyList()
   */
  val sealedClassesForEnumsMatching: List<String>?

  /**
   * Whether to generate Kotlin models with `internal` visibility modifier.
   *
   * Default value: false
   */
  val generateAsInternal: Boolean?

  /**
   * Whether to add the unknown value for enums. Unknown is used for clients when a new enum is added on the server but is not always useful
   * on servers
   */
  val addUnknownForEnums: Boolean?
  /**
   * Whether to add default arguments for input objects.
   */
  val addDefaultArgumentForInputObjects: Boolean?
  /**
   * Kotlin native generates [Any?] for optional types
   * Setting generateFilterNotNull generates extra `filterNotNull` functions that help keep the type information.
   */
  val generateFilterNotNull: Boolean?

  /**
   * Whether to generate builders in addition to constructors for operations and input types.
   * Constructors are more concise but require passing an instance of `Optional` always, making them more verbose
   * for the cases where there are a lot of optional input parameters.
   *
   * Default: false
   */
  @ApolloExperimental
  val generateInputBuilders: Boolean?

  /**
   * Whether to generate kotlin constructors with `@JvmOverloads` for more graceful Java interop experience when default values are present.
   * Note: when enabled in a multi-platform setup, the generated code can only be used in the common or JVM sourcesets.
   *
   * Default: false
   */
  val addJvmOverloads: Boolean?

  /**
   * The annotation to use for `@requiresOptIn` fields/inputFields/enumValues
   *
   * This API is itself experimental and may change without advance notice
   *
   * You can pass the special value "none" to disable adding an annotation.
   * If you're using a custom annotation, it must be able to target:
   * - AnnotationTarget.PROPERTY
   * - AnnotationTarget.CLASS
   *
   * Default: "none"
   */
  val requiresOptInAnnotation: String?

  /**
   * Whether to add the [JsExport] annotation to generated models. This is useful to be able to cast JSON parsed
   * responses into Kotlin classes using [unsafeCast].
   *
   * Default: false
   */
  @ApolloExperimental
  val jsExport: Boolean?
}

interface JavaOperationsCodegenOptions: CommonCodegenOpt, OperationsCodegenOpt, JavaCodegenOpt
interface KotlinOperationsCodegenOptions: CommonCodegenOpt, OperationsCodegenOpt, KotlinCodegenOpt
interface JavaSchemaCodegenOptions: CommonCodegenOpt, SchemaCodegenOpt, JavaCodegenOpt
interface KotlinSchemaCodegenOptions: CommonCodegenOpt, SchemaCodegenOpt, KotlinCodegenOpt

interface SchemaCodegenOptions : JavaSchemaCodegenOptions, KotlinSchemaCodegenOptions
interface OperationsCodegenOptions : JavaOperationsCodegenOptions, KotlinOperationsCodegenOptions

@Serializable
class CodegenOptions(
    override val targetLanguage: TargetLanguage?,
    override val packageName: String?,
    override val rootPackageName: String?,
    override val decapitalizeFields: Boolean?,
    override val useSemanticNaming: Boolean?,
    override val generateMethods: List<GeneratedMethod>?,
    override val operationManifestFormat: String?,
    override val generateSchema: Boolean?,
    override val generatedSchemaName: String?,
    override val sealedClassesForEnumsMatching: List<String>?,
    override val generateAsInternal: Boolean?,
    override val addUnknownForEnums: Boolean?,
    override val addDefaultArgumentForInputObjects: Boolean?,
    override val generateFilterNotNull: Boolean?,
    @ApolloExperimental override val generateInputBuilders: Boolean?,
    override val addJvmOverloads: Boolean?,
    override val requiresOptInAnnotation: String?,
    @ApolloExperimental override val jsExport: Boolean?,
    override val generateModelBuilders: Boolean?,
    override val classesForEnumsMatching: List<String>?,
    override val generatePrimitiveTypes: Boolean?,
    override val nullableFieldStyle: JavaNullable?,
    override val generateFragmentImplementations: Boolean?,
    override val generateQueryDocument: Boolean?,
): SchemaCodegenOptions, OperationsCodegenOptions

fun buildCodegenOptions(
    targetLanguage: TargetLanguage? = null,
    decapitalizeFields: Boolean? = null,
    useSemanticNaming: Boolean? = null,
    generateMethods: List<GeneratedMethod>? = null,
    operationManifestFormat: String? = null,
    generateSchema: Boolean? = null,
    generatedSchemaName: String? = null,
    sealedClassesForEnumsMatching: List<String>? = null,
    generateAsInternal: Boolean? = null,
    addUnknownForEnums: Boolean? = null,
    addDefaultArgumentForInputObjects: Boolean? = null,
    generateFilterNotNull: Boolean? = null,
    generateInputBuilders: Boolean? = null,
    addJvmOverloads: Boolean? = null,
    requiresOptInAnnotation: String? = null,
    jsExport: Boolean? = null,
    generateModelBuilders: Boolean? = null,
    classesForEnumsMatching: List<String>? = null,
    generatePrimitiveTypes: Boolean? = null,
    nullableFieldStyle: JavaNullable? = null,
    generateFragmentImplementations: Boolean? = null,
    generateQueryDocument: Boolean? = null,
    packageName: String? = null,
    rootPackageName: String? = null,
): CodegenOptions = CodegenOptions(
    targetLanguage = targetLanguage,
    decapitalizeFields = decapitalizeFields,
    useSemanticNaming = useSemanticNaming,
    generateMethods = generateMethods,
    operationManifestFormat = operationManifestFormat,
    generateSchema = generateSchema,
    generatedSchemaName = generatedSchemaName,
    sealedClassesForEnumsMatching = sealedClassesForEnumsMatching,
    generateAsInternal = generateAsInternal,
    addUnknownForEnums = addUnknownForEnums,
    addDefaultArgumentForInputObjects = addDefaultArgumentForInputObjects,
    generateFilterNotNull = generateFilterNotNull,
    generateInputBuilders = generateInputBuilders,
    addJvmOverloads = addJvmOverloads,
    requiresOptInAnnotation = requiresOptInAnnotation,
    jsExport = jsExport,
    generateModelBuilders = generateModelBuilders,
    classesForEnumsMatching = classesForEnumsMatching,
    generatePrimitiveTypes = generatePrimitiveTypes,
    nullableFieldStyle = nullableFieldStyle,
    generateFragmentImplementations = generateFragmentImplementations,
    generateQueryDocument = generateQueryDocument,
    packageName = packageName,
    rootPackageName = rootPackageName,
)

fun CodegenOptions.validate() {
  if (targetLanguage == TargetLanguage.JAVA) {
    if (generateAsInternal != null) {
      error("Apollo: generateAsInternal is not used in Java")
    }
    if (generateFilterNotNull != null) {
      error("Apollo: generateFilterNotNull is not used in Java")
    }
    if (sealedClassesForEnumsMatching != null) {
      error("Apollo: sealedClassesForEnumsMatching is not used in Java")
    }
    if (addJvmOverloads != null) {
      error("Apollo: addJvmOverloads is not used in Java")
    }
    if (requiresOptInAnnotation != null) {
      error("Apollo: requiresOptInAnnotation is not used in Java")
    }
    if (jsExport != null) {
      error("Apollo: jsExport is not used in Java")
    }
    if (generateInputBuilders != null) {
      error("Apollo: generateInputBuilders is not used in Java")
    }
  } else {
    if (nullableFieldStyle != null) {
      error("Apollo: nullableFieldStyle is not used in Kotlin")
    }
    if (generateModelBuilders != null) {
      error("Apollo: generateModelBuilders is not used in Kotlin")
    }
    if (classesForEnumsMatching != null) {
      error("Apollo: classesForEnumsMatching is not used in Kotlin")
    }
    if (generatePrimitiveTypes != null) {
      error("Apollo: generatePrimitiveTypes is not used in Kotlin")
    }
  }
}

/**
 * Controls how scalar adapters are used in the generated code.
 */
@Serializable
sealed interface AdapterInitializer

/**
 * The adapter expression will be used as-is (can be an object, a public val, a class instantiation).
 *
 * e.g. `"com.example.MyAdapter"` or `"com.example.MyAdapter()"`.
 */
@Serializable
@SerialName("expression")
class ExpressionAdapterInitializer(val expression: String) : AdapterInitializer

/**
 * The adapter instance will be looked up in the [com.apollographql.apollo.api.CustomScalarAdapters] provided at runtime.
 */
@Serializable
@SerialName("runtime")
object RuntimeAdapterInitializer : AdapterInitializer

@Serializable
class ScalarInfo(
    val targetName: String,
    val adapterInitializer: AdapterInitializer = RuntimeAdapterInitializer,
    val userDefined: Boolean = true,
)

private val NoOpLogger = object : ApolloCompiler.Logger {
  override fun warning(message: String) {
  }
}

internal val defaultAlwaysGenerateTypesMatching = emptySet<String>()
@Suppress("DEPRECATION")
internal val defaultOperationOutputGenerator = OperationOutputGenerator.Default(OperationIdGenerator.Sha256)
internal val defaultLogger = NoOpLogger
internal const val defaultUseSemanticNaming = true
internal const val defaultWarnOnDeprecatedUsages = true
internal const val defaultFailOnWarnings = false
internal const val defaultGenerateAsInternal = false
internal const val defaultGenerateFilterNotNull = false
internal const val defaultGenerateFragmentImplementations = false
internal const val defaultGenerateResponseFields = true
internal const val defaultGenerateQueryDocument = true
internal const val defaultAddTypename = ADD_TYPENAME_IF_FRAGMENTS
internal const val defaultRequiresOptInAnnotation = "none"
internal const val defaultFlattenModels = true
internal const val defaultGenerateSchema = false
internal const val defaultGeneratedSchemaName = "__Schema"
internal const val defaultGenerateDataBuilders = false
internal const val defaultGenerateModelBuilders = false
internal val defaultSealedClassesForEnumsMatching = emptyList<String>()
internal val defaultClassesForEnumsMatching = listOf(".*")
internal const val defaultGenerateOptionalOperationVariables = true
internal const val defaultAddJvmOverloads = false
internal const val defaultFieldsOnDisjointTypesMustMerge = true
internal const val defaultGeneratePrimitiveTypes = false
internal const val defaultJsExport = false
internal const val defaultGenerateInputBuilders = false
internal val defaultNullableFieldStyle = JavaNullable.NONE
internal const val defaultDecapitalizeFields = false
internal val defaultOperationManifestFormat = MANIFEST_NONE
internal val defaultAddUnkownForEnums = true
internal val defaultAddDefaultArgumentForInputObjects = true
internal val defaultCodegenModels = "operationBased"
internal val defaultTargetLanguage = TargetLanguage.KOTLIN_1_9

internal fun defaultTargetLanguage(targetLanguage: TargetLanguage?, upstreamCodegenMetadata: List<CodegenMetadata>): TargetLanguage {
  val upstreamTargetLanguage = upstreamCodegenMetadata.map { it.targetLanguage }.distinct().run {
    check(size <= 1) {
      "Apollo: inconsistent targetLanguages found: ${this.joinToString(",")}"
    }
    singleOrNull()
  }
  if (targetLanguage != null && upstreamTargetLanguage != null && targetLanguage != upstreamTargetLanguage) {
    error("Apollo: cannot depend on '$upstreamTargetLanguage' targetLanguage (expected: '${targetLanguage}').")
  }

  return targetLanguage ?: upstreamTargetLanguage ?: defaultTargetLanguage
}

internal fun defaultCodegenModels(codegenModels: String?, upstreamCodegenModels: List<String>): String {
  val upstreamCodegenModel = upstreamCodegenModels.distinct().run {
    check(size <= 1) {
      "Apollo: inconsistent codegenModels found: ${this.joinToString(",")}"
    }
    singleOrNull()
  }

  if (codegenModels != null && upstreamCodegenModel != null && codegenModels != upstreamCodegenModel) {
    error("Apollo: cannot depend on '$upstreamCodegenModel' codegenModels (expected: '${codegenModels}').")
  }

  return codegenModels ?: upstreamCodegenModel ?: defaultCodegenModels
}

internal fun generateMethodsJava(generateMethods: List<GeneratedMethod>?): List<GeneratedMethod> {
  if (generateMethods == null) {
    return listOf(GeneratedMethod.EQUALS_HASH_CODE, GeneratedMethod.TO_STRING)
  }
  check(!generateMethods.contains(GeneratedMethod.DATA_CLASS)) {
    "Java codegen does not support dataClass as an option for generateMethods"
  }
  check(!generateMethods.contains(GeneratedMethod.COPY)) {
    "Java codegen does not support copy as an option for generateMethods"
  }

  return generateMethods
}

internal fun generateMethodsKotlin(generateMethods: List<GeneratedMethod>?): List<GeneratedMethod> {
  if (generateMethods == null) {
    return listOf(GeneratedMethod.DATA_CLASS)
  }
  if (generateMethods.contains(GeneratedMethod.DATA_CLASS) && generateMethods.size > 1) {
    error("Apollo: dataClass subsumes all other method types and must be the only option passed to generateMethods")
  }
  return generateMethods
}


internal fun flattenModels(codegenModels: String): Boolean {
  return when (codegenModels) {
    MODELS_RESPONSE_BASED -> false
    else -> true
  }
}
