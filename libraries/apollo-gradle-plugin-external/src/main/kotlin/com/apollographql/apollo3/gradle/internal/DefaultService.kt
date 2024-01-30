package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.gradle.api.Introspection
import com.apollographql.apollo3.gradle.api.RegisterOperationsConfig
import com.apollographql.apollo3.gradle.api.Registry
import com.apollographql.apollo3.gradle.api.Service
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import java.io.File
import javax.inject.Inject

abstract class DefaultService @Inject constructor(val project: Project, override val name: String)
  : Service {

  internal val upstreamDependencies = mutableListOf<Dependency>()
  internal val downstreamDependencies = mutableListOf<Dependency>()

  private val objects = project.objects
  internal var registered = false
  internal var rootPackageName: String? = null

  init {
    @Suppress("LeakingThis", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    // This allows users to call includes.put("Date", "java.util.Date")
    // see https://github.com/gradle/gradle/issues/7485
    includes.convention(null as List<String>?)
    excludes.convention(null as List<String>?)
    alwaysGenerateTypesMatching.convention(null as List<String>?)
    sealedClassesForEnumsMatching.convention(null as List<String>?)
    classesForEnumsMatching.convention(null as List<String>?)
    generateMethods.convention(null as List<String>?)
    compilerJavaHooks.convention(null as List<ApolloCompilerJavaHooks>?)
    compilerKotlinHooks.convention(null as List<ApolloCompilerKotlinHooks>?)
  }

  val graphqlSourceDirectorySet = objects.sourceDirectorySet("graphql", "graphql")

  override fun srcDir(directory: Any) {
    graphqlSourceDirectorySet.srcDir(directory)
  }

  @Deprecated("Not supported any more, use dependsOn() instead", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  override fun usedCoordinates(file: File) = TODO()

  @Deprecated("Not supported any more, use dependsOn() instead", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  override fun usedCoordinates(file: String) = TODO()

  var introspection: DefaultIntrospection? = null

  override fun introspection(configure: Action<in Introspection>) {
    check(!registered) {
      "Apollo: introspection {} cannot be configured outside of a service {} block"
    }
    val introspection = objects.newInstance(DefaultIntrospection::class.java)

    if (this.introspection != null) {
      throw IllegalArgumentException("there must be only one introspection block")
    }

    configure.execute(introspection)

    if (!introspection.endpointUrl.isPresent) {
      throw IllegalArgumentException("introspection must have a url")
    }

    this.introspection = introspection
  }

  var registry: DefaultRegistry? = null

  override fun registry(configure: Action<in Registry>) {
    check(!registered) {
      "Apollo: registry {} cannot be configured outside of a service {} block"
    }

    val registry = objects.newInstance(DefaultRegistry::class.java)

    if (this.registry != null) {
      throw IllegalArgumentException("there must be only one registry block")
    }

    configure.execute(registry)

    if (!registry.graph.isPresent) {
      throw IllegalArgumentException("registry must have a graph")
    }
    if (!registry.key.isPresent) {
      throw IllegalArgumentException("registry must have a key")
    }

    this.registry = registry
  }

  var registerOperationsConfig: DefaultRegisterOperationsConfig? = null

  override fun registerOperations(configure: Action<in RegisterOperationsConfig>) {
    check(!registered) {
      "Apollo: registerOperations {} cannot be configured outside of a service {} block"
    }

    val registerOperationsConfig = objects.newInstance(DefaultRegisterOperationsConfig::class.java)

    if (this.registerOperationsConfig != null) {
      throw IllegalArgumentException("there must be only one registerOperations block")
    }

    configure.execute(registerOperationsConfig)

    this.registerOperationsConfig = registerOperationsConfig
  }

  var operationOutputAction: Action<in Service.OperationOutputConnection>? = null
  var operationManifestAction: Action<in Service.OperationManifestConnection>? = null

  @Deprecated("Use operationManifestConnection", replaceWith = ReplaceWith("operationManifestConnection"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  override fun operationOutputConnection(action: Action<in Service.OperationOutputConnection>) {
    check(!registered) {
      "Apollo: operationOutputConnection {} cannot be configured outside of a service {} block"
    }

    this.operationOutputAction = action
  }

  override fun operationManifestConnection(action: Action<in Service.OperationManifestConnection>) {
    check(!registered) {
      "Apollo: operationOutputConnection {} cannot be configured outside of a service {} block"
    }

    this.operationManifestAction = action
  }

  var outputDirAction: Action<in Service.DirectoryConnection>? = null

  override fun outputDirConnection(action: Action<in Service.DirectoryConnection>) {
    check(!registered) {
      "Apollo: outputDirConnection {} cannot be configured outside of a service {} block"
    }

    this.outputDirAction = action
  }

  override fun packageNamesFromFilePaths(rootPackageName: String?) {
    this.rootPackageName = rootPackageName ?: ""
  }

  val scalarTypeMapping = mutableMapOf<String, String>()
  val scalarAdapterMapping = mutableMapOf<String, String>()

  override fun mapScalar(
      graphQLName: String,
      targetName: String,
  ) {
    scalarTypeMapping[graphQLName] = targetName
  }

  override fun mapScalar(
      graphQLName: String,
      targetName: String,
      expression: String,
  ) {
    scalarTypeMapping[graphQLName] = targetName
    scalarAdapterMapping[graphQLName] = expression
  }

  override fun mapScalarToKotlinString(graphQLName: String) = mapScalar(graphQLName, "kotlin.String", "com.apollographql.apollo3.api.StringAdapter")
  override fun mapScalarToKotlinInt(graphQLName: String) = mapScalar(graphQLName, "kotlin.Int", "com.apollographql.apollo3.api.IntAdapter")
  override fun mapScalarToKotlinDouble(graphQLName: String) = mapScalar(graphQLName, "kotlin.Double", "com.apollographql.apollo3.api.DoubleAdapter")
  override fun mapScalarToKotlinFloat(graphQLName: String) = mapScalar(graphQLName, "kotlin.Float", "com.apollographql.apollo3.api.FloatAdapter")
  override fun mapScalarToKotlinLong(graphQLName: String) = mapScalar(graphQLName, "kotlin.Long", "com.apollographql.apollo3.api.LongAdapter")
  override fun mapScalarToKotlinBoolean(graphQLName: String) = mapScalar(graphQLName, "kotlin.Boolean", "com.apollographql.apollo3.api.BooleanAdapter")
  override fun mapScalarToKotlinAny(graphQLName: String) = mapScalar(graphQLName, "kotlin.Any", "com.apollographql.apollo3.api.AnyAdapter")

  override fun mapScalarToJavaString(graphQLName: String) = mapScalar(graphQLName, "java.lang.String", "com.apollographql.apollo3.api.Adapters.StringAdapter")
  override fun mapScalarToJavaInteger(graphQLName: String) = mapScalar(graphQLName, "java.lang.Integer", "com.apollographql.apollo3.api.Adapters.IntAdapter")
  override fun mapScalarToJavaDouble(graphQLName: String) = mapScalar(graphQLName, "java.lang.Double", "com.apollographql.apollo3.api.Adapters.DoubleAdapter")
  override fun mapScalarToJavaFloat(graphQLName: String) = mapScalar(graphQLName, "java.lang.Float", "com.apollographql.apollo3.api.Adapters.FloatAdapter")
  override fun mapScalarToJavaLong(graphQLName: String) = mapScalar(graphQLName, "java.lang.Long", "com.apollographql.apollo3.api.Adapters.LongAdapter")
  override fun mapScalarToJavaBoolean(graphQLName: String) = mapScalar(graphQLName, "java.lang.Boolean", "com.apollographql.apollo3.api.Adapters.BooleanAdapter")
  override fun mapScalarToJavaObject(graphQLName: String) = mapScalar(graphQLName, "java.lang.Object", "com.apollographql.apollo3.api.Adapters.AnyAdapter")

  override fun mapScalarToUpload(graphQLName: String) = mapScalar(graphQLName, "com.apollographql.apollo3.api.Upload", "com.apollographql.apollo3.api.UploadAdapter")

  override fun dependsOn(dependencyNotation: Any) {
    upstreamDependencies.add(project.dependencies.create(dependencyNotation))
  }

  override fun isADependencyOf(dependencyNotation: Any) {
    downstreamDependencies.add(project.dependencies.create(dependencyNotation))
  }

  internal fun isMultiModule(): Boolean = generateApolloMetadata.getOrElse(false) || downstreamDependencies.isNotEmpty() || upstreamDependencies.isNotEmpty()
  internal fun isSchemaModule(): Boolean = upstreamDependencies.isEmpty()
}

internal fun DefaultService.fallbackFiles(project: Project, block: (ConfigurableFileTree) -> Unit): FileCollection {
  val fileCollection = project.files()

  graphqlSourceDirectorySet.srcDirs.forEach { directory ->
    fileCollection.from(project.fileTree(directory, block))
  }

  return fileCollection
}

internal fun DefaultService.schemaFiles(project: Project): FileCollection {
  val fileCollection = project.files()

  @Suppress("DEPRECATION")
  if (schemaFile.isPresent) {
    project.logger.lifecycle("Apollo: using 'schemaFile.set()' is deprecated as additional schema files like 'extra.graphqls' might be required. Please use 'schemaFiles.from()' instead.")
    fileCollection.from(schemaFile)
  } else {
    fileCollection.from(schemaFiles)
  }

  return fileCollection
}

/**
 * ConfigurableFileCollections have no way to check for absent vs empty
 * [schemaFiles] can be empty at configuration time because the task responsible
 * to create the file did not run yet but still be set (because it will ultimately
 * create the file)
 *
 * The only workaround I found is to pass both to the task and defer the decision
 * which one to choose to execution time.
 *
 * See https://github.com/gradle/gradle/issues/21752
 */
internal fun DefaultService.fallbackSchemaFiles(project: Project): FileCollection {
  return fallbackFiles(project) { configurableFileTree ->
    configurableFileTree.include(listOf("**/*.graphqls", "**/*.json", "**/*.sdl"))
  }
}

/**
 * Returns a snapshot of the schema files. Some of the schema files might be missing if generated
 * from another task
 */
internal fun DefaultService.schemaFilesSnapshot(project: Project): Set<File> {
  return schemaFiles(project).files.takeIf { it.isNotEmpty() } ?: fallbackSchemaFiles(project).files
}

/**
 * Tries to guess where the schema file is.
 * This can fail when:
 * - there are several schema files.
 * - the schema file is not written yet (because it needs to be written by another task)
 */
internal fun DefaultService.guessSchemaFile(project: Project, schemaFile: RegularFileProperty): File {
  if (schemaFile.isPresent) {
    return schemaFile.get().asFile
  }
  val candidates = schemaFilesSnapshot(project)
  check(candidates.isNotEmpty()) {
    "No schema files found. Specify introspection.schemaFile or registry.schemaFile"
  }
  check(candidates.size == 1) {
    "Multiple schema files found:\n${candidates.joinToString("\n")}\n\nSpecify introspection.schemaFile or registry.schemaFile"
  }

  return candidates.single()
}