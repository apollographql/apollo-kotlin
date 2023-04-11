package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.Roots
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.defaultCodegenModels
import com.apollographql.apollo3.gradle.api.Introspection
import com.apollographql.apollo3.gradle.api.RegisterOperationsConfig
import com.apollographql.apollo3.gradle.api.Registry
import com.apollographql.apollo3.gradle.api.Service
import com.apollographql.apollo3.gradle.internal.DefaultApolloExtension.Companion.hasJavaPlugin
import com.apollographql.apollo3.gradle.internal.DefaultApolloExtension.Companion.hasKotlinPlugin
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.util.GradleVersion
import java.io.File
import javax.inject.Inject

abstract class DefaultService @Inject constructor(val project: Project, override val name: String)
  : Service {

  internal val upstreamDependencies = mutableListOf<Dependency>()
  internal val downstreamDependencies = mutableListOf<Dependency>()

  private val objects = project.objects
  var registered = false

  init {
    @Suppress("LeakingThis")
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    if (GradleVersion.current() >= GradleVersion.version("6.2")) {
      // This allows users to call includes.put("Date", "java.util.Date")
      // see https://github.com/gradle/gradle/issues/7485
      includes.convention(null as List<String>?)
      excludes.convention(null as List<String>?)
      alwaysGenerateTypesMatching.convention(null as List<String>?)
      sealedClassesForEnumsMatching.convention(null as List<String>?)
      classesForEnumsMatching.convention(null as List<String>?)
    } else {
      includes.set(null as List<String>?)
      excludes.set(null as List<String>?)
      alwaysGenerateTypesMatching.set(null as List<String>?)
      sealedClassesForEnumsMatching.set(null as List<String>?)
      classesForEnumsMatching.set(null as List<String>?)
    }
  }

  val graphqlSourceDirectorySet = objects.sourceDirectorySet("graphql", "graphql")

  override fun srcDir(directory: Any) {
    graphqlSourceDirectorySet.srcDir(directory)
  }

  @Deprecated("Not supported any more, use dependsOn() instead", level = DeprecationLevel.ERROR)
  override fun usedCoordinates(file: File) = TODO()

  @Deprecated("Not supported any more, use dependsOn() instead", level = DeprecationLevel.ERROR)
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

    generateOperationOutput.set(true)

    val registerOperationsConfig = objects.newInstance(DefaultRegisterOperationsConfig::class.java)

    if (this.registerOperationsConfig != null) {
      throw IllegalArgumentException("there must be only one registerOperations block")
    }

    configure.execute(registerOperationsConfig)

    this.registerOperationsConfig = registerOperationsConfig
  }

  var operationOutputAction: Action<in Service.OperationOutputConnection>? = null

  override fun operationOutputConnection(action: Action<in Service.OperationOutputConnection>) {
    check(!registered) {
      "Apollo: operationOutputConnection {} cannot be configured outside of a service {} block"
    }

    this.operationOutputAction = action
  }

  var outputDirAction: Action<in Service.DirectoryConnection>? = null

  override fun outputDirConnection(action: Action<in Service.DirectoryConnection>) {
    check(!registered) {
      "Apollo: outputDirConnection {} cannot be configured outside of a service {} block"
    }

    this.outputDirAction = action
  }

  override fun packageNamesFromFilePaths(rootPackageName: String?) {
    packageNameGenerator.set(
        project.provider {
          PackageNameGenerator.FilePathAware(
              roots = Roots(graphqlSourceDirectorySet.srcDirs),
              rootPackageName = rootPackageName ?: ""
          )
        }
    )
    packageNameGenerator.disallowChanges()
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

  override fun mapScalarToKotlinString(graphQLName: String) = mapScalar(graphQLName, "kotlin.String", "com.apollographql.apollo3.api.StringScalarAdapter")
  override fun mapScalarToKotlinInt(graphQLName: String) = mapScalar(graphQLName, "kotlin.Int", "com.apollographql.apollo3.api.IntScalarAdapter")
  override fun mapScalarToKotlinDouble(graphQLName: String) = mapScalar(graphQLName, "kotlin.Double", "com.apollographql.apollo3.api.DoubleScalarAdapter")
  override fun mapScalarToKotlinFloat(graphQLName: String) = mapScalar(graphQLName, "kotlin.Float", "com.apollographql.apollo3.api.FloatScalarAdapter")
  override fun mapScalarToKotlinLong(graphQLName: String) = mapScalar(graphQLName, "kotlin.Long", "com.apollographql.apollo3.api.LongScalarAdapter")
  override fun mapScalarToKotlinBoolean(graphQLName: String) = mapScalar(graphQLName, "kotlin.Boolean", "com.apollographql.apollo3.api.BooleanScalarAdapter")
  override fun mapScalarToKotlinAny(graphQLName: String) = mapScalar(graphQLName, "kotlin.Any", "com.apollographql.apollo3.api.AnyScalarAdapter")

  override fun mapScalarToJavaString(graphQLName: String) = mapScalar(graphQLName, "java.lang.String", "com.apollographql.apollo3.api.Adapters.StringScalarAdapter")
  override fun mapScalarToJavaInteger(graphQLName: String) = mapScalar(graphQLName, "java.lang.Integer", "com.apollographql.apollo3.api.Adapters.IntScalarAdapter")
  override fun mapScalarToJavaDouble(graphQLName: String) = mapScalar(graphQLName, "java.lang.Double", "com.apollographql.apollo3.api.Adapters.DoubleScalarAdapter")
  override fun mapScalarToJavaFloat(graphQLName: String) = mapScalar(graphQLName, "java.lang.Float", "com.apollographql.apollo3.api.Adapters.FloatScalarAdapter")
  override fun mapScalarToJavaLong(graphQLName: String) = mapScalar(graphQLName, "java.lang.Long", "com.apollographql.apollo3.api.Adapters.LongScalarAdapter")
  override fun mapScalarToJavaBoolean(graphQLName: String) = mapScalar(graphQLName, "java.lang.Boolean", "com.apollographql.apollo3.api.Adapters.BooleanScalarAdapter")
  override fun mapScalarToJavaObject(graphQLName: String) = mapScalar(graphQLName, "java.lang.Object", "com.apollographql.apollo3.api.Adapters.AnyScalarAdapter")

  override fun mapScalarToUpload(graphQLName: String) = mapScalar(graphQLName, "com.apollographql.apollo3.api.Upload", "com.apollographql.apollo3.api.UploadScalarAdapter")

  override fun dependsOn(dependencyNotation: Any) {
    upstreamDependencies.add(project.dependencies.create(dependencyNotation))
  }

  override fun isADependencyOf(dependencyNotation: Any) {
    downstreamDependencies.add(project.dependencies.create(dependencyNotation))
  }

  internal fun targetLanguage(): TargetLanguage {
    val generateKotlinModels: Boolean
    when {
      this.generateKotlinModels.isPresent -> {
        generateKotlinModels = this.generateKotlinModels.get()
        if (generateKotlinModels) {
          check(project.hasKotlinPlugin()) {
            "Apollo: generateKotlinModels.set(true) requires to apply a Kotlin plugin"
          }
        } else {
          check(project.hasJavaPlugin()) {
            "Apollo: generateKotlinModels.set(false) requires to apply the Java plugin"
          }
        }
      }

      project.hasKotlinPlugin() -> {
        generateKotlinModels = true
      }

      project.hasJavaPlugin() -> {
        generateKotlinModels = false
      }

      else -> {
        error("Apollo: No Java or Kotlin plugin found")
      }
    }

    return if (generateKotlinModels) {
      getKotlinTargetLanguage(this.languageVersion.orNull)
    } else {
      TargetLanguage.JAVA
    }
  }

  internal fun packageNameGenerator(): PackageNameGenerator {
    check(!(packageName.isPresent && packageNameGenerator.isPresent)) {
      "Apollo: it is an error to specify both 'packageName' and 'packageNameGenerator' "
    }
    var packageNameGenerator = this.packageNameGenerator.orNull
    if (packageNameGenerator == null) {
      packageNameGenerator = PackageNameGenerator.Flat(packageName.orNull ?: error("""
            |Apollo: specify 'packageName':
            |apollo {
            |  service("service") {
            |    packageName.set("com.example")
            |  }
            |}
          """.trimMargin()))
    }
    return packageNameGenerator
  }

  internal fun codegenModels(): String {
    return when (targetLanguage()) {
      TargetLanguage.JAVA -> {
        check(!codegenModels.isPresent || codegenModels.get() == MODELS_OPERATION_BASED) {
          "Java codegen does not support codegenModels=${codegenModels.orNull}"
        }
        MODELS_OPERATION_BASED
      }

      else -> codegenModels.getOrElse(defaultCodegenModels)
    }
  }

  internal fun alwaysGenerateTypesMatching(): Set<String> {
    if (alwaysGenerateTypesMatching.isPresent) {
      // The user specified something, use this!
      return alwaysGenerateTypesMatching.get()
    }

    if (downstreamDependencies.isEmpty()) {
      // No downstream dependency, generate everything because we don't know what types are going to be used downstream
      return setOf(".*")
    } else {
      // get the used coordinates from the downstream dependencies
      return emptySet()
    }
  }

  internal fun flattenModels(): Boolean {
    return flattenModels.getOrElse(when (codegenModels()) {
      MODELS_RESPONSE_BASED -> false
      else -> true
    })
  }
}
