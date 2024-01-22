package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MODELS_RESPONSE_BASED = "responseBased"
const val MODELS_OPERATION_BASED = "operationBased"
const val MODELS_OPERATION_BASED_WITH_INTERFACES = "experimental_operationBasedWithInterfaces"

const val ADD_TYPENAME_IF_FRAGMENTS = "ifFragments"
const val ADD_TYPENAME_IF_POLYMORPHIC = "ifPolymorphic"
const val ADD_TYPENAME_IF_ABSTRACT = "ifAbstract"
const val ADD_TYPENAME_ALWAYS = "always"

const val MANIFEST_OPERATION_OUTPUT = "operationOutput"
const val MANIFEST_PERSISTED_QUERY = "persistedQueryManifest"
const val MANIFEST_NONE = "none"

enum class TargetLanguage {
  // The order is important. See [isTargetLanguageVersionAtLeast]
  JAVA,
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
   * Fields will be generated as Apollo's `com.apollographql.apollo3.api.Optional<Type>` if nullable, or `Type` if not.
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
    val scalarMapping: Map<String, ScalarInfo> = emptyMap(),
    val generateDataBuilders: Boolean? = null,
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
    val fieldsOnDisjointTypesMustMerge: Boolean? = null,

    /**
     * Whether to decapitalize field names in the generated models (for instance `FooBar` -> `fooBar`).
     *
     * Default: false
     */
    val decapitalizeFields: Boolean? = null,

    val flattenModels: Boolean? = null,

    val warnOnDeprecatedUsages: Boolean? = null,
    val failOnWarnings: Boolean? = null,
    val addTypename: String? = null,

    /**
     * Whether to generate operation variables as [com.apollographql.apollo3.api.Optional]
     *
     * Using [com.apollographql.apollo3.api.Optional] allows to omit the variables if needed but makes the
     * callsite more verbose in most cases.
     *
     * Default: true
     */
    val generateOptionalOperationVariables: Boolean? = null,

    /**
     * Additional scalar/enum/input types to generate.
     * For input types, this will recursively add all input fields types/enums.
     */
    val alwaysGenerateTypesMatching: Set<String>? = null,

    val codegenModels: String? = null,
)


interface CommonCodegenOptions {
  val targetLanguage: TargetLanguage?

  /**
   * Whether to decapitalize field names in the generated models (for instance `FooBar` -> `fooBar`).
   *
   * Default: false
   */
  val decapitalizeFields: Boolean?

  /**
   * Which methods to auto generate on models, fragments, operations, and input objects
   */
  val generateMethods: List<GeneratedMethod>?
}

interface OperationsCodegenOptions {
  val useSemanticNaming: Boolean?

  /**
   * Whether to generate the [com.apollographql.apollo3.api.Fragment] as well as response and variables adapters.
   * If generateFragmentsAsInterfaces is true, this will also generate data classes for the fragments.
   *
   * Set to true if you need to read/write fragments from the cache or if you need to instantiate fragments
   */
  val generateFragmentImplementations: Boolean?

  /**
   * Whether to embed the query document in the [com.apollographql.apollo3.api.Operation]s. By default this is true as it is needed
   * to send the operations to the server.
   * If performance is critical, and you have a way to whitelist/read the document from another place, disable this.
   */
  val generateQueryDocument: Boolean?
}

interface SchemaCodegenOptions {
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

interface KotlinCodegenOptions {
  val generateAsInternal: Boolean?

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

interface JavaCodegenOptions {
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
   * - `apolloOptional`: Fields will be generated as Apollo's `com.apollographql.apollo3.api.Optional<Type>` if nullable, or `Type` if not.
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

interface KotlinSchemaCodegenOptions : CommonCodegenOptions, KotlinCodegenOptions, SchemaCodegenOptions {
  /**
   * A list of [Regex] patterns for GraphQL enums that should be generated as Kotlin sealed classes instead of the default Kotlin enums.
   *
   * Use this if you want your client to have access to the rawValue of the enum. This can be useful if new GraphQL enums are added but
   * the client was compiled against an older schema that doesn't have knowledge of the new enums.
   *
   * Default: emptyList()
   */
  val sealedClassesForEnumsMatching: List<String>?
}

interface KotlinOperationsCodegenOptions: CommonCodegenOptions, KotlinCodegenOptions, OperationsCodegenOptions {
  /**
   * Kotlin native will generate [Any?] for optional types
   * Setting generateFilterNotNull will generate extra `filterNotNull` functions that will help keep the type information
   */
  val generateFilterNotNull: Boolean?
}

interface KotlinResolverCodegenOptions: CommonCodegenOptions, KotlinCodegenOptions {
  val serviceName: String?
}

interface JavaSchemaCodegenOptions: CommonCodegenOptions, JavaCodegenOptions, SchemaCodegenOptions {
  /**
   * A list of [Regex] patterns for GraphQL enums that should be generated as Java classes.
   *
   * Use this if you want your client to have access to the rawValue of the enum. This can be useful if new GraphQL enums are added but
   * the client was compiled against an older schema that doesn't have knowledge of the new enums.
   *
   * Default: listOf(".*")
   */
  val classesForEnumsMatching: List<String>?
}

interface JavaOperationsCodegenOptions: CommonCodegenOptions, JavaCodegenOptions, OperationsCodegenOptions {
  /**
   * Whether to generate builders for java models
   *
   * Default value: false
   * Only valid for java models as kotlin has data classes
   */
  val generateModelBuilders: Boolean?
}

@Serializable
class CodegenOptions(
    override val targetLanguage: TargetLanguage? = null,
    override val generateMethods: List<GeneratedMethod>? = null,
    override val generateFragmentImplementations: Boolean? = null,
    override val generateQueryDocument: Boolean? = null,
    override val useSemanticNaming: Boolean? = null,
    override val generateSchema: Boolean? = null,
    override val generatedSchemaName: String? = null,
    override val sealedClassesForEnumsMatching: List<String>? = null,
    override val generateAsInternal: Boolean? = null,
    override val generateFilterNotNull: Boolean? = null,
    @ApolloExperimental override val generateInputBuilders: Boolean? = null,
    override val addJvmOverloads: Boolean? = null,
    override val requiresOptInAnnotation: String? = null,
    @ApolloExperimental override val jsExport: Boolean? = null,
    override val generateModelBuilders: Boolean? = null,
    override val classesForEnumsMatching: List<String>? = null,
    override val generatePrimitiveTypes: Boolean? = null,
    override val nullableFieldStyle: JavaNullable? = null,
    override val decapitalizeFields: Boolean? = null,
    override val serviceName: String? = null,
): JavaSchemaCodegenOptions, JavaOperationsCodegenOptions, KotlinSchemaCodegenOptions, KotlinOperationsCodegenOptions, KotlinResolverCodegenOptions

fun CodegenOptions.toSchemaCodegenOptions(): SchemaCodegenOptions {
  return SchemaCodegenOptionsImpl(
      targetLanguage = targetLanguage,
      decapitalizeFields = decapitalizeFields,
      generateMethods = generateMethods,
      generateAsInternal = generateAsInternal,
      generateInputBuilders = generateInputBuilders,
      addJvmOverloads = addJvmOverloads,
      requiresOptInAnnotation = requiresOptInAnnotation,
      jsExport = jsExport,
      generatePrimitiveTypes = generatePrimitiveTypes,
      nullableFieldStyle = nullableFieldStyle,
      classesForEnumsMatching = classesForEnumsMatching,
      sealedClassesForEnumsMatching = sealedClassesForEnumsMatching,
      generatedSchemaName = generatedSchemaName,
      generateSchema = generateSchema
  )
}

fun CodegenOptions.toOperationsCodegenOptions(): OperationsCodegenOptions {
  return OperationsCodegenOptionsImpl(
      targetLanguage = targetLanguage,
      decapitalizeFields = decapitalizeFields,
      generateMethods = generateMethods,
      useSemanticNaming = useSemanticNaming,
      generateFragmentImplementations = generateFragmentImplementations,
      generateQueryDocument = generateQueryDocument,
      generateAsInternal = generateAsInternal,
      generateInputBuilders = generateInputBuilders,
      addJvmOverloads = addJvmOverloads,
      requiresOptInAnnotation = requiresOptInAnnotation,
      jsExport = jsExport,
      generatePrimitiveTypes = generatePrimitiveTypes,
      nullableFieldStyle = nullableFieldStyle,
      generateFilterNotNull = generateFilterNotNull,
      generateModelBuilders = generateModelBuilders
  )
}

@Serializable
private class SchemaCodegenOptionsImpl(
    override val targetLanguage: TargetLanguage?,
    override val decapitalizeFields: Boolean?,
    override val generateMethods: List<GeneratedMethod>?,
    override val generateSchema: Boolean?,
    override val generatedSchemaName: String?,
    override val generateAsInternal: Boolean?,
    @ApolloExperimental override val generateInputBuilders: Boolean?,
    override val addJvmOverloads: Boolean?,
    override val requiresOptInAnnotation: String?,
    @ApolloExperimental override val jsExport: Boolean?,
    override val generatePrimitiveTypes: Boolean?,
    override val nullableFieldStyle: JavaNullable?,
    override val sealedClassesForEnumsMatching: List<String>?,
    override val classesForEnumsMatching: List<String>?,
) : JavaSchemaCodegenOptions, KotlinSchemaCodegenOptions

@Serializable
private class OperationsCodegenOptionsImpl(
    override val targetLanguage: TargetLanguage?,
    override val decapitalizeFields: Boolean?,
    override val generateMethods: List<GeneratedMethod>?,
    override val useSemanticNaming: Boolean?,
    override val generateFragmentImplementations: Boolean?,
    override val generateQueryDocument: Boolean?,
    override val generateAsInternal: Boolean?,
    @ApolloExperimental override val generateInputBuilders: Boolean?,
    override val addJvmOverloads: Boolean?,
    override val requiresOptInAnnotation: String?,
    @ApolloExperimental override val jsExport: Boolean?,
    override val generatePrimitiveTypes: Boolean?,
    override val nullableFieldStyle: JavaNullable?,
    override val generateFilterNotNull: Boolean?,
    override val generateModelBuilders: Boolean?
) : JavaOperationsCodegenOptions, KotlinOperationsCodegenOptions

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
 * The adapter instance will be looked up in the [com.apollographql.apollo3.api.CustomScalarAdapters] provided at runtime.
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
internal val defaultOperationOutputGenerator = OperationOutputGenerator.Default
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
internal val defaultCodegenModels = MODELS_OPERATION_BASED
internal val defaultTargetLanguage = TargetLanguage.KOTLIN_1_9

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


fun CodegenOptions.validate() {
  if (targetLanguage == TargetLanguage.JAVA) {
    if (generateAsInternal != null) {
      error("Apollo: generateAsInternal is not used in Java")
    }
    if (generateFilterNotNull != null) {
      error("Apollo: generateFilterNotNull is not used in Java")
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
    if (generatePrimitiveTypes != null) {
      error("Apollo: generatePrimitiveTypes is not used in Kotlin")
    }
  }
}