package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.OperationIdGenerator
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.Roots
import com.apollographql.apollo3.gradle.api.Introspection
import com.apollographql.apollo3.gradle.api.Registry
import com.apollographql.apollo3.gradle.api.Service
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.util.GradleVersion
import javax.inject.Inject

abstract class DefaultService @Inject constructor(val project: Project, override val name: String)
  : Service {

  val objects = project.objects
  init {
    if (GradleVersion.current().compareTo(GradleVersion.version("6.2")) >= 0) {
      // This allows users to call customScalarsMapping.put("Date", "java.util.Date")
      // see https://github.com/gradle/gradle/issues/7485
      customScalarsMapping.convention(null as Map<String, String>?)
      include.convention(null as List<String>?)
      exclude.convention(null as List<String>?)
      alwaysGenerateTypesMatching.convention(null as Set<String>?)
    } else {
      customScalarsMapping.set(null as Map<String, String>?)
      include.set(null as List<String>?)
      exclude.set(null as List<String>?)
      alwaysGenerateTypesMatching.set(null as Set<String>?)
    }
  }

  abstract override val exclude: ListProperty<String>

  abstract override val include: ListProperty<String>

  abstract override val sourceFolder: Property<String>

  abstract override val schemaFile: RegularFileProperty
  abstract override val schemaFiles: ConfigurableFileCollection

  abstract override val debugDir: DirectoryProperty
  abstract override val outputDir: DirectoryProperty

  abstract override val generateOperationOutput: Property<Boolean>
  abstract override val operationOutputFile: RegularFileProperty

  abstract override val warnOnDeprecatedUsages: Property<Boolean>

  abstract override val failOnWarnings: Property<Boolean>

  abstract override val customScalarsMapping: MapProperty<String, String>

  abstract override val operationIdGenerator: Property<OperationIdGenerator>

  abstract override val operationOutputGenerator: Property<OperationOutputGenerator>

  abstract override val packageName: Property<String>
  abstract override val packageNameGenerator: Property<PackageNameGenerator>

  abstract override val useSemanticNaming: Property<Boolean>

  abstract override val generateAsInternal: Property<Boolean>

  abstract override val generateKotlinModels: Property<Boolean>

  abstract override val generateApolloMetadata: Property<Boolean>

  abstract override val alwaysGenerateTypesMatching: SetProperty<String>

  abstract override val generateFragmentImplementations: Property<Boolean>

  abstract override val codegenModels: Property<String>

  abstract override val flattenModels: Property<Boolean>

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

  var operationOutputAction: Action<in Service.OperationOutputWire>? = null

  override fun withOperationOutput(action: Action<in Service.OperationOutputWire>) {
    this.operationOutputAction = action
    generateOperationOutput.set(true)
  }

  var outputDirAction: Action<in Service.OutputDirWire>? = null

  override fun withOutputDir(action: Action<in Service.OutputDirWire>) {
    this.outputDirAction = action
  }

  override fun filePathAwarePackageNameGenerator(rootPackageName: String?) {
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
