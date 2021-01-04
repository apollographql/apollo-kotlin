package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.OperationOutputGenerator
import com.apollographql.apollo.gradle.api.Introspection
import com.apollographql.apollo.gradle.api.Registry
import com.apollographql.apollo.gradle.api.Service
import com.apollographql.apollo.gradle.internal.DefaultApolloExtension.Companion.MIN_GRADLE_VERSION
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.util.GradleVersion
import javax.inject.Inject

abstract class DefaultService @Inject constructor(val objects: ObjectFactory, override val name: String)
  : Service {

  init {
    if (GradleVersion.current().compareTo(GradleVersion.version("6.2")) >= 0) {
      // This allows users to call customScalarsMapping.put("Date", "java.util.Date")
      // see https://github.com/gradle/gradle/issues/7485
      customScalarsMapping.convention(null as Map<String, String>?)
      sealedClassesForEnumsMatching.convention(null as List<String>?)
      include.convention(null as List<String>?)
      exclude.convention(null as List<String>?)
      alwaysGenerateTypesMatching.convention(null as Set<String>?)
    } else {
      customScalarsMapping.set(null as Map<String, String>?)
      sealedClassesForEnumsMatching.set(null as List<String>?)
      include.set(null as List<String>?)
      exclude.set(null as List<String>?)
      alwaysGenerateTypesMatching.set(null as Set<String>?)
    }
  }

  abstract override val sourceFolder: Property<String>

  abstract override val exclude: ListProperty<String>

  abstract override val include: ListProperty<String>

  abstract override val schemaFile: RegularFileProperty

  abstract override val warnOnDeprecatedUsages: Property<Boolean>

  abstract override val failOnWarnings: Property<Boolean>

  abstract override val customScalarsMapping: MapProperty<String, String>

  abstract override val operationIdGenerator: Property<OperationIdGenerator>

  abstract override val operationOutputGenerator: Property<OperationOutputGenerator>

  abstract override val useSemanticNaming: Property<Boolean>

  abstract override val rootPackageName: Property<String>

  abstract override val generateAsInternal: Property<Boolean>

  abstract override val sealedClassesForEnumsMatching: ListProperty<String>

  abstract override val generateApolloMetadata: Property<Boolean>

  abstract override val alwaysGenerateTypesMatching: SetProperty<String>

  val graphqlSourceDirectorySet = objects.sourceDirectorySet("graphql", "graphql")

  override fun addGraphqlDirectory(directory: Any) {
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
  }

  var outputDirAction: Action<in Service.OutputDirWire>? = null

  override fun withOutputDir(action: Action<in Service.OutputDirWire>) {
    this.outputDirAction = action
  }

  fun resolvedSchemaProvider(project: Project): Provider<RegularFile> {
    return schemaFile.orElse(project.layout.file(project.provider {
      val candidates = graphqlSourceDirectorySet.srcDirs.flatMap { srcDir ->
        srcDir.walkTopDown().filter { it.name == "schema.json" || it.name == "schema.sdl" }.toList()
      }

      check(candidates.size <= 1) {
        """
Multiple schemas found:
${candidates.joinToString(separator = "\n")}
Multiple schemas are not supported. You can either define multiple services or specify the schema you want to use explicitely with `schemaFile`
        """.trimIndent()
      }

      candidates.firstOrNull()
    }))
  }
}
