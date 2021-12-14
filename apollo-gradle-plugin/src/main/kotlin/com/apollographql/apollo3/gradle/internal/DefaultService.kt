package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.compiler.MODELS_COMPAT
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.Roots
import com.apollographql.apollo3.gradle.api.Introspection
import com.apollographql.apollo3.gradle.api.RegisterOperationsConfig
import com.apollographql.apollo3.gradle.api.Registry
import com.apollographql.apollo3.gradle.api.Service
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import javax.inject.Inject

@OptIn(ApolloExperimental::class)
abstract class DefaultService @Inject constructor(val project: Project, override val name: String)
  : Service {

  val objects = project.objects
  init {
    @Suppress("LeakingThis")
    if (GradleVersion.current() >= GradleVersion.version("6.2")) {
      // This allows users to call customScalarsMapping.put("Date", "java.util.Date")
      // see https://github.com/gradle/gradle/issues/7485
      customScalarsMapping.convention(null as Map<String, String>?)
      customTypeMapping.convention(null as Map<String, String>?)
      includes.convention(null as List<String>?)
      excludes.convention(null as List<String>?)
      alwaysGenerateTypesMatching.convention(null as Set<String>?)
      sealedClassesForEnumsMatching.convention(null as List<String>?)
    } else {
      customScalarsMapping.set(null as Map<String, String>?)
      customTypeMapping.set(null as Map<String, String>?)
      includes.set(null as List<String>?)
      excludes.set(null as List<String>?)
      alwaysGenerateTypesMatching.set(null as Set<String>?)
      sealedClassesForEnumsMatching.set(null as List<String>?)
    }
  }

  val graphqlSourceDirectorySet = objects.sourceDirectorySet("graphql", "graphql")

  override fun srcDir(directory: Any) {
    graphqlSourceDirectorySet.srcDir(directory)
  }

  var introspection: DefaultIntrospection? = null

  override fun introspection(configure: Action<in Introspection>) {
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
    this.operationOutputAction = action
  }

  var outputDirAction: Action<in Service.DirectoryConnection>? = null
  var testDirAction: Action<in Service.DirectoryConnection>? = null

  override fun outputDirConnection(action: Action<in Service.DirectoryConnection>) {
    this.outputDirAction = action
  }

  override fun useVersion2Compat(rootPackageName: String?) {
    packageNamesFromFilePaths(rootPackageName)
    codegenModels.set(MODELS_COMPAT)
  }

  override fun testDirConnection(action: Action<in Service.DirectoryConnection>) {
    this.testDirAction = action
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
}
