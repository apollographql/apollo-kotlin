package com.apollographql.apollo3.gradle.api

import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_1
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_8_3
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.compiler.OperationIdGenerator
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.hooks.internal.AddInternalCompilerHooks
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
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
   * The plugin will look in "src/main/graphql/$sourceFolder" for Android/JVM projects and "src/commonMain/graphql/$sourceFolder" for multiplatform projects.
   *
   * For more control, see also [srcDir]
   */
  val sourceFolder: Property<String>

  /**
   * Adds the given directory as a GraphQL source root
   *
   * Use [srcDir] if your files are outside "src/main/graphql" or to have them in multiple folders.
   *
   * @param directory the directory where the .graphql operation files are
   * [directory] is evaluated as in [Project.file](https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#file-java.lang.Object-)
   * Valid value include path Strings, File and RegularFileProperty
   *
   */
  fun srcDir(directory: Any)

  /**
   * A shorthand property that will be used if [schemaFiles] is empty
   */
  val schemaFile: RegularFileProperty

  /**
   * The schema files as either a ".json" introspection schema or a ".sdl|.graphqls" SDL schema. You might come across schemas named "schema.graphql",
   * these are SDL schemas most of the time that need to be renamed to "schema.graphqls" to be recognized properly.
   *
   * The compiler accepts multiple schema files in order to add extensions to specify key fields and other schema extensions.
   *
   * By default, the plugin collects all "schema.[json|sdl|graphqls]" file in the source roots
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
   * For custom scalar types like Date, map from the GraphQL type to the Java/Kotlin type.
   *
   * Default value: the empty map
   */
  @Deprecated("Use mapScalar() instead")
  @ApolloDeprecatedSince(v3_0_1)
  val customScalarsMapping: MapProperty<String, String>

  @Deprecated("customTypeMapping is a helper property to help migrating to 3.x " +
      "and will be removed in a future version. Use mapScalar() instead.")
  @ApolloDeprecatedSince(v3_0_0)
  val customTypeMapping: MapProperty<String, String>

  /**
   * Map a GraphQL scalar type to the Java/Kotlin type.
   * The adapter must be configured at runtime via [com.apollographql.apollo3.ApolloClient.Builder.addCustomScalarAdapter].
   *
   * @param graphQLName: the name of the scalar to map as found in the GraphQL schema
   * @param targetName: the fully qualified Java or Kotlin name of the type the scalar is mapped to
   *
   * For example: `mapScalar("Date", "com.example.Date")`
   */
  fun mapScalar(graphQLName: String, targetName: String)

  /**
   * Map a GraphQL scalar type to the Java/Kotlin type and provided adapter expression.
   * The adapter will be configured at compile time and you must not call [com.apollographql.apollo3.ApolloClient.Builder.addCustomScalarAdapter].
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
   * Map the given GraphQL scalar to [com.apollographql.apollo3.api.Upload] and use the builtin adapter
   */
  fun mapScalarToUpload(graphQLName: String)

  /**
   * By default, Apollo uses `Sha256` hashing algorithm to generate an ID for the query.
   * To provide a custom ID generation logic, pass an `instance` that implements the [OperationIdGenerator]. How the ID is generated is
   * indifferent to the compiler. It can be a hashing algorithm or generated by a backend.
   *
   * Example Md5 hash generator:
   * ```groovy
   * import com.apollographql.apollo3.compiler.OperationIdGenerator
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
  val operationIdGenerator: Property<OperationIdGenerator>

  /**
   * A generator to generate the operation output from a list of operations.
   * OperationOutputGenerator is similar to [OperationIdGenerator] but can work on lists. This is useful if you need
   * to register/whitelist your operations on your server all at once.
   *
   * Example Md5 hash generator:
   * ```groovy
   * import com.apollographql.apollo3.compiler.OperationIdGenerator
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
  val operationOutputGenerator: Property<OperationOutputGenerator>

  /**
   * When true, the generated classes names will end with 'Query' or 'Mutation'.
   * If you write `query droid { ... }`, the generated class will be named 'DroidQuery'.
   *
   * Default value: true
   */
  val useSemanticNaming: Property<Boolean>

  /**
   * The package name of the models. The compiler will generate classes in
   *
   * - $packageName/SomeQuery.kt
   * - $packageName/fragment/SomeFragment.kt
   * - $packageName/type/CustomScalar.kt
   * - $packageName/type/SomeInputObject.kt
   * - $packageName/type/SomeEnum.kt
   *
   * Default value: ""
   *
   * See also [packageNamesFromFilePaths]
   */
  val packageName: Property<String>

  /**
   * Use [packageNameGenerator] to customize how to generate package names from file paths.
   *
   * See [PackageNameGenerator] for more details
   *
   * See also [packageNamesFromFilePaths]
   */
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
   * Whether to use the schema package name for fragments. This is used for backward compat with 2.x
   *
   * Default value: false
   */
  @Deprecated("Used for backward compat with 2.x, will be removed in a future version")
  @ApolloDeprecatedSince(v3_8_3)
  val useSchemaPackageNameForFragments: Property<Boolean>

  /**
   * Whether to generate kotlin constructors with `@JvmOverloads` for more graceful Java interop experience when default values are present.
   * Note: when enabled in a multi-platform setup, the generated code can only be used in the common or JVM sourcesets.
   *
   * Default value: false
   */
  val addJvmOverloads: Property<Boolean>

  /**
   * Whether to generate Kotlin models with `internal` visibility modifier.
   *
   * To specify which classes to generate as `internal`, [compilerKotlinHooks] with [AddInternalCompilerHooks]
   * can be used instead.
   *
   * Default value: false
   */
  val generateAsInternal: Property<Boolean>

  /**
   * Whether to generate Apollo metadata. Apollo metadata is used for multi-module support. Set this to true if you want other
   * modules to be able to re-use fragments and types from this module.
   *
   * This is currently experimental and this API might change in the future.
   *
   * Default value: false
   */
  val generateApolloMetadata: Property<Boolean>

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
   * Whether to generate default implementation classes for GraphQL fragments.
   * Default value is `false`, means only interfaces are generated.
   *
   * Most of the time, fragment implementations are not needed because you can easily access fragments interfaces and read all
   * data from your queries. They are needed if you want to be able to build fragments outside an operation. For an exemple
   * to programmatically build a fragment that is reused in another part of your code or to read and write fragments to the cache.
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
   * Must be either "1.4" or "1.5".
   *
   * Note: "1.4", while still supported, is not useful since Apollo Kotlin requires Kotlin 1.5+. It is considered deprecated.
   *
   * Using an higher languageVersion allows generated code to use more language features like
   * sealed interfaces in Kotlin 1.5 for an example.
   *
   * See also https://kotlinlang.org/docs/gradle.html#attributes-common-to-jvm-and-js
   *
   * Default: use the version of the Kotlin plugin.
   */
  val languageVersion: Property<String>

  /**
   * Whether to write the query document in models
   */
  val generateQueryDocument: Property<Boolean>

  /**
   * Whether to generate the Schema class. The Schema class lists all composite
   * types in order to access __typename and/or possibleTypes.
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
   * Whether to generate operation variables as [com.apollographql.apollo3.api.Optional]
   *
   * Using [com.apollographql.apollo3.api.Optional] allows to omit the variables if needed but makes the
   * callsite more verbose in most cases.
   *
   * Default: true
   */
  val generateOptionalOperationVariables: Property<Boolean>

  /**
   * Whether to generate the type safe Data builders. These are mainly used for tests but can also be used for other use
   * cases too.
   *
   * Only valid when [generateKotlinModels] is true
   */
  @ApolloExperimental
  @Deprecated("Test builders are operation based and generate a lot of code. Use data builders instead", ReplaceWith("generateDataBuilders"))
  val generateTestBuilders: Property<Boolean>

  /**
   * Whether to generate the type safe Data builders. These are mainly used for tests but can also be used for other use
   * cases too.
   */
  @ApolloExperimental
  val generateDataBuilders: Property<Boolean>

  /**
   * Whether to generate response model builders for Java.
   *
   * Default: false
   */
  @ApolloExperimental
  @Deprecated("use generateModelBuilders instead", ReplaceWith("generateModelBuilders"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_6_3)
  val generateModelBuilder: Property<Boolean>

  /**
   * Whether to generate response model builders for Java.
   *
   * Default: false
   */
  @ApolloExperimental
  val generateModelBuilders: Property<Boolean>

  /**
   * What codegen to use. One of "operationBased", "responseBased", "compat" or "experimental_operationBasedWithInterfaces"
   *
   * - "operationBased" generates models that map 1:1 with the GraphQL operation
   * - "responseBased" generates models that map 1:1 with the Json response
   * - "compat" is for compatibility with 2.x and will be removed in a future version
   * - "experimental_operationBasedWithInterfaces" is like "operationBased" except it will generate an interface for selection
   * sets that contain fragments to make it easier to use `when` statements
   *
   * Default value: "operationBased"
   */
  val codegenModels: Property<String>

  /**
   * When to add __typename. One of "always", "ifFragments", "ifAbstract" or "ifPolymorphic"
   *
   * - "always": Add '__typename' for every composite field
   *
   * - "ifFragments": Add '__typename' for every selection set that contains fragments (inline or named)
   * This causes cache misses when introducing fragments where no fragment was present before and will be certainly removed in
   * a future version.
   *
   * - "ifAbstract": Add '__typename' for abstract fields, i.e. fields that are of union or interface type
   * Note: It also adds '__typename' on fragment definitions that satisfy the same property because fragments
   * could be read from the cache, and we don't have a containing field in that case.
   *
   * - "ifPolymorphic": Add '__typename' for polymorphic fields, i.e. fields that contains a subfragment
   * (inline or named) whose type condition isn't a super type of the field type.
   * If a field is monomorphic, no '__typename' will be added.
   * This adds the bare minimum amount of __typename but the logic is substantially more complex and
   * it could cause cache misses when using fragments on monomorphic fields because __typename can be
   * required in some cases.
   *
   * Note: It also adds '__typename' on fragment definitions that satisfy the same property because fragments
   * could be read from the cache, and we don't have a containing field in that case.
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
   * The directory where the test builders will be written.
   * If you want a [DirectoryProperty] that carries the task dependency, use [outputDirConnection]
   */
  @Deprecated("Test builders are operation based and generate a lot of code. Use data builders instead")
  @ApolloDeprecatedSince(v3_8_3)
  val testDir: DirectoryProperty

  /**
   * Whether to generate the operationOutput.json
   *
   * Defaults value: false
   */
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
   * A list of [Regex] patterns for GraphQL enums that should be generated as Kotlin sealed classes instead of the default Kotlin enums.
   *
   * Use this if you want your client to have access to the rawValue of the enum. This can be useful if new GraphQL enums are added but
   * the client was compiled against an older schema that doesn't have knowledge of the new enums.
   *
   * Only valid when [generateKotlinModels] is `true`
   *
   * Default: emptyList()
   */
  val sealedClassesForEnumsMatching: ListProperty<String>

  /**
   * A list of [Regex] patterns for GraphQL enums that should be generated as Java classes.
   *
   * Use this if you want your client to have access to the rawValue of the enum. This can be useful if new GraphQL enums are added but
   * the client was compiled against an older schema that doesn't have knowledge of the new enums.
   *
   * Only valid when [generateKotlinModels] is `false`
   *
   * Default: listOf(".*")
   */
  val classesForEnumsMatching: ListProperty<String>

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
   * Only valid when [generateKotlinModels] is `false`
   *
   * Default: false
   */
  val generatePrimitiveTypes: Property<Boolean>

  /**
   * The style to use for fields that are nullable in the Java generated code.
   *
   * Only valid when [generateKotlinModels] is `false`.
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
  val nullableFieldStyle: Property<String>

  /**
   * Whether to decapitalize field names in the generated models (for instance `FooBar` -> `fooBar`).
   *
   * Default: false
   */
  val decapitalizeFields: Property<Boolean>

  /**
   * Hooks to customize the generated Kotlin code.
   *
   * See [ApolloCompilerKotlinHooks] for more details.
   *
   * Only valid when [generateKotlinModels] is `true`
   *
   * Note: use the `com.apollographql.apollo3.external` Gradle plugin instead of `com.apollographql.apollo3` to use this,
   * so the KotlinPoet classes are available in the classpath.
   */
  @ApolloExperimental
  val compilerKotlinHooks: ListProperty<ApolloCompilerKotlinHooks>

  /**
   * Hooks to customize the generated Java code.
   *
   * See [ApolloCompilerJavaHooks] for more details.
   *
   * Only valid when [generateKotlinModels] is `false`
   *
   * Note: use the `com.apollographql.apollo3.external` Gradle plugin instead of `com.apollographql.apollo3` to use this,
   * so the JavaPoet classes are available in the classpath.
   */
  @ApolloExperimental
  val compilerJavaHooks: ListProperty<ApolloCompilerJavaHooks>

  /**
   * A shorthand method that configures defaults that match Apollo Android 2.x codegen
   *
   * In practice, it does the following:
   *
   * ```
   * packageNamesFromFilePaths(rootPackageName)
   * useSchemaPackageNameForFragments.set(true)
   * codegenModels.set(MODELS_COMPAT)
   * ```
   *
   * See the individual options for a more complete description.
   *
   * This method is deprecated and provided for migration purposes only. It will be removed
   * in a future version
   */
  @Deprecated("useVersion2Compat() is a helper function to help migrating to 3.x " +
      "and will be removed in a future version")
  @ApolloDeprecatedSince(v3_0_0)
  fun useVersion2Compat(rootPackageName: String? = null)

  /**
   * By default, the used coordinates are computed automatically from all modules sharing a schema. This means that
   * changing a query in a module might trigger task execution in a completely different module. If your used coordinates
   * do not change too often, you can opt-in to save them in a file that is read instead of checking all modules.
   *
   * This is more performant but more error prone because you'll need to keep this file up to date by calling `generate${service}ApolloUsedCoordinates`
   *
   * We recommend checking the file in source control. For an example in `src/main/graphql/used-coordinates.json`
   *
   * @param file A Json file containing an array of GraphQL coordinates
   *
   */
  fun usedCoordinates(file: File)

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
  fun operationOutputConnection(action: Action<in OperationOutputConnection>)

  /**
   * overrides the way the operation manifest is connected.
   * Use this if you want to connect the generated operation manifest. For an example
   * you can use this to send the modified queries to your backend for whitelisting
   *
   * By default, operation manifest is not connected
   */
  fun operationManifestConnection(action: Action<in OperationManifestConnection>)

  class OperationOutputConnection(
      /**
       * The task that produces operationOutput
       */
      val task: TaskProvider<out Task>,

      /**
       * A json file containing a [Map]<[String], [com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor]>
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
   * Overrides the way the generated test builders are connected.
   * Use this if you want to connect the generated test builders to another task than the default destination.
   *
   * By default, the generated sources are connected to:
   * - test sourceSet for Kotlin projects
   * - commonTest sourceSet for Kotlin multiplatform projects
   * - test *and* androidTest variants for Android projects
   */
  @Deprecated("Test builders are operation based and generate a lot of code. Use data builders instead")
  @ApolloDeprecatedSince(v3_8_3)
  fun testDirConnection(action: Action<in DirectoryConnection>)

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
     * Connects the generated sources to all the Android variants
     * Throws if the Android plugin is not applied
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
}
