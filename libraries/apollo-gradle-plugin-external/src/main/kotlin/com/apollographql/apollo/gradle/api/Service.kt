@file:Suppress("DEPRECATION")

package com.apollographql.apollo.gradle.api

import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.compiler.MANIFEST_PERSISTED_QUERY
import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.OperationOutputGenerator
import com.apollographql.apollo.compiler.PackageNameGenerator
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.TaskProvider
import java.io.File

/**
 * A [Service] represents a GraphQL schema and associated queries.
 *
 * The queries will be compiled and verified against the schema to generate the models.
 */
interface Service {
  val name: String

  /**
   * Operation files to include.
   * The values are interpreted as in [org.gradle.api.tasks.util.PatternFilterable]
   *
   * Default: listOf("**&#47;*.graphql", "**&#47;*.gql")
   */
  val includes: ListProperty<String>

  /**
   * Operation files to exclude.
   * The values are interpreted as in [org.gradle.api.tasks.util.PatternFilterable]
   *
   * Default: emptyList()
   */
  val excludes: ListProperty<String>

  /**
   * Where to look for GraphQL sources.
   * The plugin searches "src/main/graphql/$sourceFolder" for Android/JVM projects and "src/commonMain/graphql/$sourceFolder" for multiplatform projects.
   *
   * For more control, see [srcDir]
   */
  @Deprecated("Replace with srcDir(\"src/[main|commonMain]/graphql/\$sourceFolder\")")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val sourceFolder: Property<String>

  /**
   * Adds the given directory as a GraphQL source root
   *
   * Use [srcDir] if your files are outside "src/main/graphql" or to have them in multiple folders.
   *
   * @param directory the directory where the .graphql operation files are
   * [directory] is evaluated as in [Project.file](https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#file-java.lang.Object-)
   */
  fun srcDir(directory: Any)

  /**
   * The location of the main schema file.
   */
  val schemaFile: RegularFileProperty

  /**
   * The schema files in either ".json" introspection schemas or ".sdl|.graphqls" SDL schemas.
   *
   * Most of the time, [schemaFiles] contains a main schema and an optional separate file for client extensions.
   * You may set any number of files and the compiler will merge them. The merge order is:
   * - main schema file first (the main schema file is the file that contains the schema definition or the Query type)
   * - lexicographic order of the filename for other schema files
   *
   * If not set or empty, the plugin collects all "*.[json|sdl|graphqls]" files in the [srcDir] roots.
   *
   * Example:
   *
   * ```kotlin
   * schemaFiles.from("src/main/graphql/schema.graphqls", "src/main/graphql/extra.graphqls")
   * ```
   */
  val schemaFiles: ConfigurableFileCollection

  /**
   * Warn if using a deprecated field
   *
   * Default value: true
   */
  val warnOnDeprecatedUsages: Property<Boolean>

  /**
   * Fail the build if there are warnings. This is not named `allWarningAsErrors` to avoid nameclashes with the Kotlin options
   *
   * Default value: false
   */
  val failOnWarnings: Property<Boolean>

  /**
   * Map a GraphQL scalar type to the Java/Kotlin type.
   * The adapter must be configured at runtime via [com.apollographql.apollo.ApolloClient.Builder.addCustomScalarAdapter].
   *
   * @param graphQLName: the name of the scalar to map as found in the GraphQL schema
   * @param targetName: the fully qualified Java or Kotlin name of the type the scalar is mapped to
   *
   * For example: `mapScalar("Date", "com.example.Date")`
   */
  fun mapScalar(graphQLName: String, targetName: String)

  /**
   * Map a GraphQL scalar type to the Java/Kotlin type and provided adapter expression.
   * The adapter will be configured at compile time and you must not call [com.apollographql.apollo.ApolloClient.Builder.addCustomScalarAdapter].
   *
   * @param graphQLName: the name of the scalar to map as found in the GraphQL schema
   * @param targetName: the fully qualified Java or Kotlin name of the type the scalar is mapped to
   * @param expression: an expression that will be used by the codegen to get an adapter for the
   * given scalar. [expression] is passed verbatim to JavaPoet/KotlinPoet.
   *
   * For example in Kotlin:
   * - `mapScalar("Date", "com.example.Date", "com.example.DateAdapter")` (a top level property or object)
   * - `mapScalar("Date", "com.example.Date", "com.example.DateAdapter()")` (create a new instance every time)
   * Or in Java:
   * - `mapScalar("Date", "com.example.Date", "com.example.DateAdapter.INSTANCE")` (a top level property or object)
   * - `mapScalar("Date", "com.example.Date", "new com.example.DateAdapter()")` (create a new instance every time)
   */
  fun mapScalar(graphQLName: String, targetName: String, expression: String)

  /**
   * Map the given GraphQL scalar to [kotlin.String] and use the builtin adapter
   */
  fun mapScalarToKotlinString(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [kotlin.Int] and use the builtin adapter
   */
  fun mapScalarToKotlinInt(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [kotlin.Double] and use the builtin adapter
   */
  fun mapScalarToKotlinDouble(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [kotlin.Float] and use the builtin adapter
   */
  fun mapScalarToKotlinFloat(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [kotlin.Long] and use the builtin adapter
   */
  fun mapScalarToKotlinLong(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [kotlin.Boolean] and use the builtin adapter
   */
  fun mapScalarToKotlinBoolean(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [kotlin.Any] and use the builtin adapter
   */
  fun mapScalarToKotlinAny(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [java.lang.String] and use the builtin adapter
   */
  fun mapScalarToJavaString(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [java.lang.Integer] and use the builtin adapter
   */
  fun mapScalarToJavaInteger(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [java.lang.Double] and use the builtin adapter
   */
  fun mapScalarToJavaDouble(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [java.lang.Float] and use the builtin adapter
   */
  fun mapScalarToJavaFloat(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [java.lang.Long] and use the builtin adapter
   */
  fun mapScalarToJavaLong(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [java.lang.Boolean] and use the builtin adapter
   */
  fun mapScalarToJavaBoolean(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [java.lang.Object] and use the builtin adapter
   */
  fun mapScalarToJavaObject(graphQLName: String)

  /**
   * Map the given GraphQL scalar to [com.apollographql.apollo.api.Upload] and use the builtin adapter
   */
  fun mapScalarToUpload(graphQLName: String)

  /**
   * By default, Apollo uses `Sha256` hashing algorithm to generate an ID for the query.
   * To provide a custom ID generation logic, pass an `instance` that implements the [OperationIdGenerator]. How the ID is generated is
   * indifferent to the compiler. It can be a hashing algorithm or generated by a backend.
   *
   * Example Md5 hash generator:
   * ```groovy
   * import com.apollographql.apollo.compiler.OperationIdGenerator
   *
   * apollo {
   *   service("service") {
   *     operationIdGenerator = new OperationIdGenerator() {
   *       String apply(String operationDocument, String operationFilepath) {
   *         return operationDocument.md5()
   *       }
   *
   *       /**
   *        * Use this version override to indicate an update to the implementation.
   *        * This invalidates the current cache.
   *        */
   *       String version = "v1"
   *     }
   *   }
   * }
   * ```
   *
   * Default value: [OperationIdGenerator.Sha256]
   */
  @Suppress("DEPRECATION")
  @Deprecated("Use ApolloCompilerPlugin.operationIds() instead. See https://go.apollo.dev/ak-compiler-plugins for more details.")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val operationIdGenerator: Property<OperationIdGenerator>

  /**
   * A generator to generate the operation output from a list of operations.
   * OperationOutputGenerator is similar to [OperationIdGenerator] but can work on lists. This is useful if you need
   * to register/whitelist your operations on your server all at once.
   *
   * Example Md5 hash generator:
   * ```groovy
   * import com.apollographql.apollo.compiler.OperationIdGenerator
   *
   * apollo {
   *   service("service") {
   *     operationOutputGenerator = new OperationIdGenerator() {
   *       String apply(List<operation operationDocument, String operationFilepath) {
   *         return operationDocument.md5()
   *       }
   *
   *       /**
   *        * Use this version override to indicate an update to the implementation.
   *        * This invalidates the current cache.
   *        */
   *       String version = "v1"
   *     }
   *   }
   * }
   * ```
   *
   * Default value: [OperationIdGenerator.Sha256]
   */
  @Suppress("DEPRECATION")
  @Deprecated("Use ApolloCompilerPlugin.operationIds() instead. See https://go.apollo.dev/ak-compiler-plugins for more details.")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val operationOutputGenerator: Property<OperationOutputGenerator>

  /**
   * When true, the operation class names are suffixed with their operation type like ('Query', 'Mutation' ot 'Subscription').
   * For an example, `query getDroid { ... }` GraphQL query generates the 'GetDroidQuery' class.
   *
   * Default value: true
   */
  val useSemanticNaming: Property<Boolean>

  /**
   * The package name for generated classes. Operations use the package name directly. Other
   * classes like fragments and schema types use sub-packages to avoid name clashes:
   *
   * - $packageName/SomeQuery.kt
   * - $packageName/fragment/SomeFragment.kt
   * - $packageName/type/CustomScalar.kt
   * - etc...
   */
  val packageName: Property<String>

  /**
   * Use [packageNameGenerator] to customize how to generate package names from file paths.
   *
   * See [PackageNameGenerator] for more details
   *
   * @see [packageName]
   */
  @Deprecated("Use ApolloCompilerPlugin.layout() instead. See https://go.apollo.dev/ak-compiler-plugins for more details.")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val packageNameGenerator: Property<PackageNameGenerator>

  /**
   * A helper method to configure a [PackageNameGenerator] that will use the file path
   * relative to the source roots to generate the packageNames
   *
   * @param rootPackageName: a root package name to prepend to the package names
   *
   * Example, with the below configuration:
   *
   * ```
   * srcDir("src/main/graphql")
   * packageNamesFromFilePaths("com.example")
   * ```
   *
   * an operation defined in `src/main/graphql/query/feature1` will use `com.example.query.feature1`
   * as package name
   * an input object defined in `src/main/graphql/schema/schema.graphqls` will use `com.example.schema.type`
   * as package name
   */
  fun packageNamesFromFilePaths(rootPackageName: String? = null)

  /**
   * Whether to generate kotlin constructors with `@JvmOverloads` for more graceful Java interop experience when default values are present.
   * Note: when enabled in a multi-platform setup, the generated code can only be used in the common or JVM sourcesets.
   *
   * Only valid if [generateKotlinModels] is true.
   *
   * Default value: false
   */
  val addJvmOverloads: Property<Boolean>

  /**
   * Whether to generate Kotlin models with `internal` visibility modifier.
   *
   * Only valid when [generateKotlinModels] is true.
   *
   * Default value: false
   */
  val generateAsInternal: Property<Boolean>

  /**
   * Whether to generate Apollo metadata. Apollo metadata is used for multimodule support. Set this to true if you want other
   * modules to be able to re-use fragments and types from this module.
   *
   * This is currently experimental and this API might change in the future.
   *
   * Default value: false
   */
  val generateApolloMetadata: Property<Boolean>

  /**
   * Whether to add the [JsExport] annotation to generated models. This is useful to be able to cast JSON parsed
   * responses into Kotlin classes using [unsafeCast].
   *
   * Only valid if [generateKotlinModels] is true.
   *
   * This is currently experimental and this API might change in the future.
   *
   * Default: false
   */
  val jsExport: Property<Boolean>

  /**
   * A list of [Regex] patterns matching [schema coordinates](https://github.com/magicmark/graphql-spec/blob/add_field_coordinates/rfcs/SchemaCoordinates.md)
   * for types and fields that should be generated whether they are used by queries/fragments in this module or not.
   *
   * When using multiple modules, Apollo Kotlin will generate all the types by default in the root module
   * because the root module doesn't know what types are going to be used by dependent modules. This can be prohibitive in terms
   * of compilation speed for large projects. If that's the case, opt-in the types that are used by multiple dependent modules here.
   * You don't need to add types that are used by a single dependent module.
   *
   * Examples:
   * - listOf(".*"): generate every type and every field in the schema
   * - listOf("User"): generate the user type
   * - listOf(".*User): generate all types ending with "User"
   * - listOf("User\\..*"): generate all fields of type "User"
   *
   * Default value: if (generateApolloMetadata) listOf(".*") else listOf()
   */
  val alwaysGenerateTypesMatching: SetProperty<String>

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
  val generateFragmentImplementations: Property<Boolean>

  /**
   * Whether to generate Kotlin or Java models
   * Default to true if the Kotlin plugin is found
   */
  val generateKotlinModels: Property<Boolean>

  /**
   * Target language version for the generated code.
   *
   * Only valid when [generateKotlinModels] is `true`.
   *
   * Possible values:
   * - "1.5" (deprecated): base language version
   * - "1.9": same as "1.5" but uses `entries` instead of `values()` in enums
   *
   * Other versions might be added later as the Kotlin language evolves.
   *
   * Default: use the Kotlin Gradle plugin version
   */
  val languageVersion: Property<String>

  /**
   * Whether to embed the query document in the [com.apollographql.apollo.api.Operation]s. By default, this is true as it is needed
   * to send the operations to the server.
   * If performance/binary size is critical, and you are using persisted queries or a similar mechanism, disable this.
   *
   * Default: true
   */
  val generateQueryDocument: Property<Boolean>

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
  val generateSchema: Property<Boolean>

  /**
   * Class name to use when generating the Schema class.
   *
   * Default: "__Schema"
   */
  val generatedSchemaName: Property<String>

  /**
   * Whether to generate operation variables as [com.apollographql.apollo.api.Optional]
   *
   * Using [com.apollographql.apollo.api.Optional] allows to omit the variables if needed but makes the
   * callsite more verbose in most cases.
   *
   * Default: true
   */
  val generateOptionalOperationVariables: Property<Boolean>

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
  val generateMethods: ListProperty<String>

  /**
   * Whether to generate the type safe Data builders. These are mainly used for tests but can also be used for other use
   * cases too.
   */
  @ApolloExperimental
  val generateDataBuilders: Property<Boolean>

  /**
   * Whether to generate builders for java models
   *
   * Only valid when [generateKotlinModels] is `false`.
   *
   * Default value: false
   */
  @ApolloExperimental
  val generateModelBuilders: Property<Boolean>

  /**
   * What codegen to use. One of "operationBased", "responseBased" or "experimental_operationBasedWithInterfaces"
   *
   * - "operationBased" generates models that map 1:1 with the GraphQL operation
   * - "responseBased" generates models that map 1:1 with the Json response
   * - "experimental_operationBasedWithInterfaces" is like "operationBased" except it will generate an interface for selection
   * sets that contain fragments to make it easier to use `when` statements
   *
   * Default value: "operationBased"
   */
  val codegenModels: Property<String>

  /**
   * When to add __typename. One of "always", "ifAbstract", "ifPolymorphic" or "ifFragments"
   *
   * - "always": Add '__typename' for every composite field
   *
   * - "ifAbstract": Add '__typename' for abstract fields, i.e. fields that are of union or interface type
   *
   * - "ifPolymorphic": Add '__typename' for polymorphic fields, i.e. fields that contains a subfragment
   * (inline or named) whose type condition isn't a super type of the field type.
   * If a field is monomorphic, no '__typename' will be added.
   * This adds the bare minimum amount of __typename but the logic is substantially more complex than `ifAbstract`.
   *
   * - "ifFragments" (deprecated): Add '__typename' for every selection set that contains fragments (inline or named)
   * This causes cache misses when introducing fragments where no fragment was present before. This is deprecated and
   * will be removed in a future version.
   *
   * Apollo Kotlin requires __typename to handle polymorphism and parsing fragments. By default, __typename is added on
   * every composite field selection set. When using the cache, this also ensures that cache keys can read __typename.
   * If you're not using the cache or do not use __typename in your cache keys, you can use "ifAbstract" or "ifPolymorphic"
   * to reduce the number of __typename and the size of the network response.
   *
   * Default value: "ifFragments"
   */
  val addTypename: Property<String>

  /**
   * Whether to flatten the models. File paths are limited on MacOSX to 256 chars and flattening can help keeping the path length manageable
   * The drawback is that some classes may nameclash in which case they will be suffixed with a number
   *
   * Default value: false for "responseBased", true else
   */
  val flattenModels: Property<Boolean>

  /**
   * The directory where the generated models will be written. It's called [outputDir] but this an "input" parameter for the compiler
   * If you want a [DirectoryProperty] that carries the task dependency, use [outputDirConnection]
   */
  val outputDir: DirectoryProperty
  
  /**
   * Whether to generate the operationOutput.json
   *
   * Defaults value: false
   */
  @Deprecated("Use operationManifestFormat", ReplaceWith("operationManifestFormat.set(\"$MANIFEST_PERSISTED_QUERY\")"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val generateOperationOutput: Property<Boolean>

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
  val operationManifestFormat: Property<String>

  /**
   * The file where the operation output will be written. It's called [operationOutputFile] but this an "input" parameter for the compiler
   * If you want a [RegularFileProperty] that carries the task dependency, use [operationManifestConnection]
   */
  @Deprecated("Use operationManifest", ReplaceWith("operationManifest"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val operationOutputFile: RegularFileProperty

  /**
   * The file where to write the operation manifest.
   * If you want a [RegularFileProperty] that carries the task dependency, use [operationManifestConnection].
   */
  val operationManifest: RegularFileProperty

  /**
   * A debug directory where the compiler will output intermediary results
   */
  val debugDir: DirectoryProperty

  /**
   * A list of [Regex] patterns for GraphQL enums that should be generated as a Kotlin sealed interface.
   *
   * This provides several benefits over the default of mapping GraphQL enums to Kotlin enums:
   * - the client can access the string value of unknown values (enum values added on the server after the client has been compiled).
   * - it introduces an intermediate `KNOWN__` type that does not contain the unknown value for the cases where you want to map all unknown values to a known one.
   * - it's harder to create instances of `UNKNOWN__` values, making it more explicit that those values are dangerous to be used as input.
   *
   * Only valid when [generateKotlinModels] is `true`.
   *
   * Default: `emptyList()`
   */
  val sealedClassesForEnumsMatching: ListProperty<String>

  /**
   * A list of [Regex] patterns for GraphQL enums that should be generated as Java classes.
   *
   * Only valid when [generateKotlinModels] is `false`.
   *
   * Use this if you want your client to have access to the rawValue of the enum. This can be useful if new GraphQL enums are added but
   * the client was compiled against an older schema that doesn't have knowledge of the new enums.
   *
   * Default: `listOf(".*")`
   */
  val classesForEnumsMatching: ListProperty<String>

  /**
   * The annotation to use for `@requiresOptIn` fields/inputFields/enumValues
   *
   * Only valid if [generateKotlinModels] is true.
   *
   * This API is itself experimental and may change without advance notice
   *
   * You can pass the special value "none" to disable adding an annotation.
   * If you're using a custom annotation, it must be able to target:
   * - [AnnotationTarget.PROPERTY]
   * - [AnnotationTarget.CLASS]
   *
   * Default: "none"
   */
  @ApolloExperimental
  val requiresOptInAnnotation: Property<String>

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
  val fieldsOnDisjointTypesMustMerge: Property<Boolean>

  /**
   * Whether to generate fields as primitive types (`int`, `double`, `boolean`) instead of their boxed types (`Integer`, `Double`,
   * `Boolean`) when possible.
   *
   * Only valid when [generateKotlinModels] is `false`.
   *
   * Default: false
   */
  val generatePrimitiveTypes: Property<Boolean>

  /**
   * Whether to generate builders in addition to constructors for operations and input types.
   * Constructors are more concise but require passing an instance of `Optional` always, making them more verbose
   * for the cases where there are a lot of optional input parameters.
   *
   * Only valid if [generateKotlinModels] is true
   *
   * Default: false
   */
  @ApolloExperimental
  val generateInputBuilders: Property<Boolean>

  /**
   * The style to use for fields that are nullable in the Java generated code.
   *
   * Only valid when [generateKotlinModels] is `false`.
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
  val nullableFieldStyle: Property<String>

  /**
   * Whether to decapitalize fields  (for instance `FooBar` -> `fooBar`). This is useful if your schema has fields starting with an uppercase as
   * it may create name clashes with models that use PascalCase
   *
   * Default: false
   */
  val decapitalizeFields: Property<Boolean>

  @Deprecated("Not supported any more, use dependsOn() instead", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  fun usedCoordinates(file: File)
  @Deprecated("Not supported any more, use dependsOn() instead", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  fun usedCoordinates(file: String)

  /**
   * Configures [Introspection] to download an introspection Json schema
   */
  fun introspection(configure: Action<in Introspection>)

  /**
   * Configures [Registry] to download a SDL schema from a studio registry
   */
  fun registry(configure: Action<in Registry>)

  /**
   * Configures operation safelisting (requires an [Apollo Studio](https://www.apollographql.com/docs/studio/) account)
   */
  fun registerOperations(configure: Action<in RegisterOperationsConfig>)

  /**
   * overrides the way operationOutput is connected.
   * Use this if you want to connect the generated operationOutput. For an example
   * you can use this to send the modified queries to your backend for whitelisting
   *
   * By default, operationOutput is not connected
   */
  @Deprecated("Use operationManifestConnection", ReplaceWith("operationManifestConnection"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  fun operationOutputConnection(action: Action<in OperationOutputConnection>)

  /**
   * overrides the way the operation manifest is connected.
   * Use this if you want to connect the generated operation manifest. For an example
   * you can use this to send the modified queries to your backend for whitelisting
   *
   * By default, operation manifest is not connected
   */
  fun operationManifestConnection(action: Action<in OperationManifestConnection>)

  /**
   * Adds a dependency for the codegen. Use this when some types/fragments are generated
   * in upstream modules.
   */
  fun dependsOn(dependencyNotation: Any)

  /**
   * Counterpoint of [dependsOn]. [isADependencyOf] allows a schema module to discover
   * used types in downstream modules automatically without having to specify [alwaysGenerateTypesMatching].
   *
   * This works by setting a dependency on the IR in downstream modules.
   * Because the IR task and the codegen task are separate this does not create a cycle.
   *
   * @see [alwaysGenerateTypesMatching]
   */
  fun isADependencyOf(dependencyNotation: Any)

  /**
   * Same as [dependsOn] but allows for automatic detection of used types.
   * This works by adding the current project to the upstream project dependencies using cross-project configuration.
   *
   * This function is experimental and not compatible with [Gradle project isolation](https://docs.gradle.org/current/userguide/isolated_projects.html).
   *
   * @param bidirectional if true and if [dependencyNotation] is a project dependency,
   * automatically configure the symmetrical dependency for automatic detection of used types.
   */
  @ApolloExperimental
  fun dependsOn(dependencyNotation: Any, bidirectional: Boolean)

  class OperationOutputConnection(
      /**
       * The task that produces operationOutput
       */
      val task: TaskProvider<out Task>,

      /**
       * A json file containing a [Map]<[String], [com.apollographql.apollo.compiler.operationoutput.OperationDescriptor]>
       *
       * This file can be used to upload the queries exact content and their matching operation ID to a server for whitelisting
       * or persisted queries.
       */
      val operationOutputFile: Provider<RegularFile>,
  )

  class OperationManifestConnection(
      /**
       * The task that produces operationOutput
       */
      val task: TaskProvider<out Task>,

      /**
       * A json file containing the operation manifest
       *
       * This file can be used to upload the queries exact content and their matching operation ID to a server for whitelisting
       * or persisted queries. The specific format of the file depends on [operationManifestFormat]
       */
      val manifest: Provider<RegularFile>,
  )

  /**
   * Overrides the way the generated models are connected.
   * Use this if you want to connect the generated models to another task than the default destination.
   *
   * By default, the generated sources are connected to:
   * - main sourceSet for Kotlin projects
   * - commonMain sourceSet for Kotlin multiplatform projects
   * - main sourceSet for Android projects
   */
  fun outputDirConnection(action: Action<in DirectoryConnection>)

  /**
   * A [DirectoryConnection] defines how the generated sources are connected to the rest of the
   * build.
   *
   * It provides helpers for the most common options as well as direct access to an output [Provider]
   * that will carry task dependency.
   *
   * It is valid to call multiple connectXyz() methods to connect the generated sources to multiple
   * downstream tasks
   */
  interface DirectoryConnection {
    /**
     * Connects the generated sources to the given Kotlin source set.
     * Throws if the Kotlin plugin is not applied
     *
     * @param name: the name of the source set. For an example, "commonTest"
     */
    fun connectToKotlinSourceSet(name: String)

    /**
     * Connects the generated sources to the given Java source set.
     * Throws if the Java plugin is not applied
     *
     * @param name: the name of the source set. For an example, "test"
     */
    fun connectToJavaSourceSet(name: String)

    /**
     * Connects the generated sources to the given Android source set.
     * Throws if the Android plugin is not applied
     *
     * @param name: the name of the source set. For an example, "main", "test" or "androidTest"
     * You can also use more qualified source sets like "demo", "debug" or "demoDebug"
     */
    fun connectToAndroidSourceSet(name: String)

    /**
     * Connects the generated sources to the main Android variants.
     * Note: despite the name, this method does not connect to the test variants as the main
     * classpath is accessible from the tests and adding the sources twice causes issues.
     * See https://issuetracker.google.com/u/1/issues/268218176
     *
     * @throws Exception if the Android plugin is not applied
     */
    fun connectToAllAndroidVariants()

    /**
     * Connects the generated sources to the given Android variant. This will
     * look up the most specific source set used by this variant. For an example, "demoDebug"
     *
     * @param variant: the [BaseVariant] to connect to. It is of type [Any] because [DirectoryConnection]
     * can be used in non-Android projects, and we don't want the class to fail during loading because
     * of a missing symbol in that case
     */
    fun connectToAndroidVariant(variant: Any)

    /**
     * The directory where the generated models will be written.
     * This provider carries task dependency information.
     */
    val outputDir: Provider<Directory>


    /**
     * The task that produces outputDir. Usually this is not needed as [outputDir] carries
     * task dependency.
     */
    val task: TaskProvider<out Task>
  }

  fun plugin(dependencyNotation: Any)

  @ApolloExperimental
  fun plugin(dependencyNotation: Any, block: Action<CompilerPlugin>)
}
