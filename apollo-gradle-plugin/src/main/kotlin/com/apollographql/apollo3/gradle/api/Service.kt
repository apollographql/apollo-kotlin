package com.apollographql.apollo3.gradle.api

import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_1
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.compiler.OperationIdGenerator
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.PackageNameGenerator
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
   * The adapter must be configured at runtime via `ApolloClient.Builder.addCustomScalarAdapter()`.
   *
   * For example: `mapScalar("Date", "com.example.Date")`
   */
  fun mapScalar(graphQLName: String, targetName: String)

  /**
   * Map a GraphQL scalar type to the Java/Kotlin type and provided adapter expression.
   *
   * For example:
   * - `mapScalar("Date", "com.example.Date", "com.example.DateAdapter")` (an instance property or object)
   * - `mapScalar("Date", "com.example.Date", "com.example.DateAdapter()")` (instantiate the class on the fly)
   */
  fun mapScalar(graphQLName: String, targetName: String, expression: String)

  fun mapScalarToKotlinString(graphQLName: String)
  fun mapScalarToKotlinInt(graphQLName: String)
  fun mapScalarToKotlinDouble(graphQLName: String)
  fun mapScalarToKotlinFloat(graphQLName: String)
  fun mapScalarToKotlinLong(graphQLName: String)
  fun mapScalarToKotlinBoolean(graphQLName: String)
  fun mapScalarToKotlinAny(graphQLName: String)

  fun mapScalarToJavaString(graphQLName: String)
  fun mapScalarToJavaInteger(graphQLName: String)
  fun mapScalarToJavaDouble(graphQLName: String)
  fun mapScalarToJavaFloat(graphQLName: String)
  fun mapScalarToJavaLong(graphQLName: String)
  fun mapScalarToJavaBoolean(graphQLName: String)
  fun mapScalarToJavaObject(graphQLName: String)

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
   *   operationIdGenerator = new OperationIdGenerator() {
   *     String apply(String operationDocument, String operationFilepath) {
   *       return operationDocument.md5()
   *     }
   *
   *     /**
   *      * Use this version override to indicate an update to the implementation.
   *      * This invalidates the current cache.
   *      */
   *     String version = "v1"
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
   *   operationOutputGenerator = new OperationIdGenerator() {
   *     String apply(List<operation operationDocument, String operationFilepath) {
   *       return operationDocument.md5()
   *     }
   *
   *     /**
   *      * Use this version override to indicate an update to the implementation.
   *      * This invalidates the current cache.
   *      */
   *     String version = "v1"
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
   */
  val packageName: Property<String>

  /**
   * Use [packageNameGenerator] to customize how to generate package names from file paths.
   *
   * See [PackageNameGenerator] for more details
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
  val useSchemaPackageNameForFragments: Property<Boolean>

  /**
   * Whether to generate Kotlin models with `internal` visibility modifier.
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
   * A list of [Regex] patterns for input/scalar/enum types that should be generated whether they are used by queries/fragments
   * in this module. When using multiple modules, Apollo Kotlin will generate all the types by default in the root module
   * because the root module doesn't know what types are going to be used by dependent modules. This can be prohibitive in terms
   * of compilation speed for large projects. If that's the case, opt-in the types that are used by multiple dependent modules here.
   * You don't need to add types that are used by a single dependent module.
   *
   * This is currently experimental and this API might change in the future.
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
   * Only valid when [generateKotlinModels] is `true`
   * Must be either "1.4" or "1.5"
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
   * Whether to generate the __Schema class. The __Schema class lists all composite
   * types in order to access __typename and/or possibleTypes
   */
  val generateSchema: Property<Boolean>

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
  val generateTestBuilders: Property<Boolean>

  /**
   * What codegen to use. One of "operationBased", "responseBased" or "compat"
   *
   * Default value: "operationBased"
   */
  val codegenModels: Property<String>

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
  val testDir: DirectoryProperty

  /**
   * Whether to generate the operationOutput.json
   *
   * Defaults value: false
   */
  val generateOperationOutput: Property<Boolean>

  /**
   * The file where the operation output will be written. It's called [operationOutputFile] but this an "input" parameter for the compiler
   * If you want a [RegularFileProperty] that carries the task dependency, use [operationOutputConnection]
   */
  val operationOutputFile: RegularFileProperty

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
   * Default: emptyList()
   */
  val sealedClassesForEnumsMatching: ListProperty<String>

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
   * Configures [Introspection] to download an introspection Json schema
   */
  fun introspection(configure: Action<in Introspection>)

  /**
   * Configures [Registry] to download a SDL schema from a studio registry
   */
  fun registry(configure: Action<in Registry>)

  /**
   * Configures the [Introspection]
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
