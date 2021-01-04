package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.OperationOutputGenerator
import com.apollographql.apollo.gradle.api.AndroidProject
import com.apollographql.apollo.gradle.api.ApolloAttributes
import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.KotlinJvmProject
import com.apollographql.apollo.gradle.api.KotlinMultiplatformProject
import com.apollographql.apollo.gradle.api.Service
import com.apollographql.apollo.gradle.api.androidExtension
import com.apollographql.apollo.gradle.api.isKotlinMultiplatform
import com.apollographql.apollo.gradle.api.kotlinJvmExtension
import com.apollographql.apollo.gradle.api.kotlinMultiplatformExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

abstract class DefaultApolloExtension(private val project: Project, private val defaultService: DefaultService) : ApolloExtension, Service by defaultService {

  private val services = mutableListOf<DefaultService>()
  private val checkVersionsTask: TaskProvider<Task>
  private val apolloConfiguration: Configuration
  private val rootProvider: TaskProvider<Task>
  private var registerDefaultService = true

  // Called when the plugin is applied
  init {
    require(GradleVersion.current().compareTo(GradleVersion.version(MIN_GRADLE_VERSION)) >= 0) {
      "apollo-android requires Gradle version ${MIN_GRADLE_VERSION} or greater"
    }

    apolloConfiguration = project.configurations.create(ModelNames.apolloConfiguration()) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = false

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
      }
    }

    checkVersionsTask = registerCheckVersionsTask()

    /**
     * An aggregate task to easily generate all models
     */
    rootProvider = project.tasks.register(ModelNames.generateApolloSources()) {
      it.group = TASK_GROUP
      it.description = "Generate Apollo models for all services"
    }

    /**
     * A simple task to be used from the command line to ease the schema download
     */
    project.tasks.register(ModelNames.downloadApolloSchema(), ApolloDownloadSchemaTask::class.java) { task ->
      task.group = TASK_GROUP
    }
    /**
     * A simple task to be used from the command line to ease the schema upload
     */
    project.tasks.register(ModelNames.pushApolloSchema(), ApolloPushSchemaTask::class.java) { task ->
      task.group = TASK_GROUP
    }
    /**
     * A simple task to be used from the command line to ease schema conversion
     */
    project.tasks.register(ModelNames.convertApolloSchema(), ApolloConvertSchemaTask::class.java) { task ->
      task.group = TASK_GROUP
    }
    project.afterEvaluate {
      if (registerDefaultService) {
        registerService(defaultService)
      } else {
        check(defaultService.graphqlSourceDirectorySet.isEmpty
            && defaultService.sourceFolder.isPresent.not()
            && defaultService.schemaFile.isPresent.not()
            && defaultService.alwaysGenerateTypesMatching.isPresent.not()
            && defaultService.customScalarsMapping.isPresent.not()
            && defaultService.exclude.isPresent.not()
            && defaultService.include.isPresent.not()
            && defaultService.failOnWarnings.isPresent.not()
            && defaultService.generateApolloMetadata.isPresent.not()
            && defaultService.generateAsInternal.isPresent.not()
            && defaultService.sealedClassesForEnumsMatching.isPresent.not()
        ) {
          """
            Configuring the default service is ignored if you specify other services, remove your configuration from the root of the apollo {} block:
            apollo {
              // remove everything at the top level
              
              // add individual services
              service("service1") {
                // ...
              }
              service("service2") {
                // ...
              }
            }
          """.trimIndent()
        }
      }
    }
  }

  /**
   * Call from users to explicitly register a service or by the plugin to register the implicit service
   */
  override fun service(name: String, action: Action<Service>) {
    registerDefaultService = false

    val service = project.objects.newInstance(DefaultService::class.java, project.objects, name)
    action.execute(service)

    registerService(service)
  }

  private fun registerCheckVersionsTask(): TaskProvider<Task> {
    return project.tasks.register(ModelNames.checkApolloVersions()) {
      val outputFile = BuildDirLayout.versionCheck(project)

      val allDeps = (
          getDeps(project.rootProject.buildscript.configurations) +
              getDeps(project.buildscript.configurations) +
              getDeps(project.configurations)
          )

      val allVersions = allDeps.mapNotNull { it.version }.distinct().sorted()
      it.inputs.property("allVersions", allVersions)
      it.outputs.file(outputFile)

      it.doLast {
        check(allVersions.size <= 1) {
          val found = allDeps.map { "${it.name}:${it.version}" }.distinct().joinToString("\n")
          "All apollo versions should be the same. Found:\n$found"
        }

        val version = allVersions.firstOrNull()
        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeText("All versions are consistent: $version")
      }
    }
  }

  private fun registerService(service: DefaultService) {
    check(services.find { it.name == name } == null) {
      "There is already a service named $name, please use another name"
    }
    services.add(service)

    val producerConfigurationName = ModelNames.producerConfiguration(service)

    project.configurations.create(producerConfigurationName) {
      it.isCanBeConsumed = true
      it.isCanBeResolved = false

      it.extendsFrom(apolloConfiguration)

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
        it.attribute(ApolloAttributes.APOLLO_SERVICE_ATTRIBUTE, project.objects.named(ApolloAttributes.Service::class.java, service.name))
      }
    }

    val consumerConfiguration = project.configurations.create(ModelNames.consumerConfiguration(service)) {
      it.isCanBeResolved = true
      it.isCanBeConsumed = false

      it.extendsFrom(apolloConfiguration)

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
        it.attribute(ApolloAttributes.APOLLO_SERVICE_ATTRIBUTE, project.objects.named(ApolloAttributes.Service::class.java, service.name))
      }
    }

    val codegenProvider = registerCodeGenTask(project, service, consumerConfiguration)

    project.artifacts {
      it.add(producerConfigurationName, codegenProvider.flatMap { it.metadataOutputFile })
    }

    codegenProvider.configure {
      it.dependsOn(checkVersionsTask)
      it.dependsOn(consumerConfiguration)
    }

    val checkApolloDuplicates = maybeRegisterCheckDuplicates(project.rootProject, service)

    // Add project dependency on root project to this project, with our new configurations
    project.rootProject.dependencies.apply {
      add(
          ModelNames.duplicatesConsumerConfiguration(service),
          project(mapOf("path" to project.path, "configuration" to producerConfigurationName))
      )
    }

    codegenProvider.configure {
      it.finalizedBy(checkApolloDuplicates)
    }

    if (service.operationOutputAction != null) {
      val operationOutputWire = Service.OperationOutputWire(
          task = codegenProvider,
          operationOutputFile = codegenProvider.flatMap { it.operationOutputFile }
      )
      service.operationOutputAction!!.execute(operationOutputWire)
    }

    val outputDirWire = Service.OutputDirWire(
        task = codegenProvider,
        outputDir = codegenProvider.flatMap { it.outputDir }
    )
    if (service.outputDirAction == null) {
      service.outputDirAction = mainWireAction
    }

    service.outputDirAction!!.execute(outputDirWire)

    rootProvider.configure {
      it.dependsOn(codegenProvider)
    }

    registerDownloadSchemaTasks(service)
  }

  /**
   * The default wiring.
   */
  val mainWireAction = Action<Service.OutputDirWire> { wire ->
    when {
      project.kotlinMultiplatformExtension != null -> KotlinMultiplatformProject.registerGeneratedDirectoryToCommonMainSourceSet(project, wire)
      project.androidExtension != null -> AndroidProject.registerGeneratedDirectoryToAllVariants(project, wire)
      project.kotlinJvmExtension != null -> KotlinJvmProject.registerGeneratedDirectoryToMainSourceSet(project, wire)
      else -> throw IllegalStateException("Cannot find the Kotlin extension, please apply a kotlin plugin")
    }
  }

  private fun maybeRegisterCheckDuplicates(rootProject: Project, service: Service): TaskProvider<ApolloCheckDuplicatesTask> {
    val taskName = ModelNames.checkApolloDuplicates(service)
    return try {
      rootProject.tasks.named(taskName) as TaskProvider<ApolloCheckDuplicatesTask>
    } catch (e: Exception) {
      val configuration = rootProject.configurations.create(ModelNames.duplicatesConsumerConfiguration(service)) {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false

        it.attributes {
          it.attribute(Usage.USAGE_ATTRIBUTE, rootProject.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
          it.attribute(ApolloAttributes.APOLLO_SERVICE_ATTRIBUTE, rootProject.objects.named(ApolloAttributes.Service::class.java, service.name))
        }
      }

      rootProject.tasks.register(taskName, ApolloCheckDuplicatesTask::class.java) {
        it.outputFile.set(BuildDirLayout.duplicatesCheck(rootProject, service))
        it.metadataFiles.from(configuration)
      }
    }
  }

  private fun registerCodeGenTask(project: Project, service: DefaultService, consumerConfiguration: Configuration): TaskProvider<ApolloGenerateSourcesTask> {
    return project.tasks.register(ModelNames.generateApolloSources(service), ApolloGenerateSourcesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo models for ${service.name} GraphQL queries"


      if (service.graphqlSourceDirectorySet.isReallyEmpty) {
        val folder = service.sourceFolder.getOrElse(".")
        val dir = File(project.projectDir, "src/${mainSourceSet(project)}/graphql").resolve(folder)

        service.graphqlSourceDirectorySet.srcDir(dir)
      }
      service.graphqlSourceDirectorySet.include(service.include.getOrElse(listOf("**/*.graphql", "**/*.gql")))
      service.graphqlSourceDirectorySet.exclude(service.exclude.getOrElse(emptyList()))

      task.graphqlFiles.setFrom(service.graphqlSourceDirectorySet)
      // Since this is stored as a list of string, the order matter hence the sorting
      task.rootFolders.set(project.provider { service.graphqlSourceDirectorySet.srcDirs.map { it.relativeTo(project.projectDir).path }.sorted() })
      // This has to be lazy in case the schema is not written yet during configuration
      // See the `graphql files can be generated by another task` test
      task.schemaFile.set(service.resolvedSchemaProvider(project))
      task.operationOutputGenerator = service.operationOutputGenerator.getOrElse(
          OperationOutputGenerator.DefaultOperationOuputGenerator(
              service.operationIdGenerator.orElse(OperationIdGenerator.Sha256()).get()
          )
      )

      task.useSemanticNaming.set(service.useSemanticNaming)
      task.generateKotlinModels.set(true)
      task.warnOnDeprecatedUsages.set(service.warnOnDeprecatedUsages)
      task.failOnWarnings.set(service.failOnWarnings)
      task.customScalarsMapping.set(service.customScalarsMapping)
      task.outputDir.apply {
        set(BuildDirLayout.sources(project, service))
        disallowChanges()
      }
      if (service.operationOutputAction != null) {
        task.operationOutputFile.apply {
          set(BuildDirLayout.operationOuput(project, service))
          disallowChanges()
        }
      }
      // always set `metadataOutputFile` as the `metadata` task is part of `assemble` (see https://github.com/gradle/gradle/issues/14065)
      // and we don't want it to fail if it is ever called by the user
      task.metadataOutputFile.apply {
        set(BuildDirLayout.metadata(project, service))
        disallowChanges()
      }

      task.metadataFiles.from(consumerConfiguration)

      task.rootPackageName.set(service.rootPackageName)
      task.generateAsInternal.set(service.generateAsInternal)
      task.generateFilterNotNull.set(project.isKotlinMultiplatform)
      task.sealedClassesForEnumsMatching.set(service.sealedClassesForEnumsMatching)
      task.alwaysGenerateTypesMatching.set(service.alwaysGenerateTypesMatching)
      task.projectName.set(project.name)
    }
  }

  private fun registerDownloadSchemaTasks(service: DefaultService) {
    val introspection = service.introspection
    if (introspection != null) {
      project.tasks.register(ModelNames.downloadApolloSchemaIntrospection(service), ApolloDownloadSchemaTask::class.java) { task ->

        task.group = TASK_GROUP
        task.endpoint.set(introspection.endpointUrl)
        task.header = introspection.headers.get().map { "${it.key}: ${it.value}" }
        task.schema.set(service.resolvedSchemaProvider(project).map { it.asFile.absolutePath })
      }
    }
    val registry = service.registry
    if (registry != null) {
      project.tasks.register(ModelNames.downloadApolloSchemaIntrospection(service), ApolloDownloadSchemaTask::class.java) { task ->

        task.group = TASK_GROUP
        task.graph.set(registry.graph)
        task.key.set(registry.key)
        task.graphVariant.set(registry.graphVariant)
        task.schema.set(service.resolvedSchemaProvider(project).map { it.asFile.absolutePath })
      }
    }
  }

  override fun createAllAndroidVariantServices(suffix: String, action: Action<Service>) {
    /**
     * The android plugin will call us back when the variants are ready but before that happens, disable the default service
     */
    registerDefaultService = false

    AndroidProject.onEachVariant(project = project, withTestVariants = true) { variant ->
      val name = "${variant.name}${suffix.capitalize()}"

      val service = project.objects.newInstance(DefaultService::class.java, project.objects, name)
      action.execute(service)

      if (service.graphqlSourceDirectorySet.isReallyEmpty) {
        val sourceFolder = service.sourceFolder.getOrElse(".")
        check(!File(sourceFolder).isRooted && !sourceFolder.startsWith("..")) {
          """ApolloGraphQL: using 'sourceFolder = "$sourceFolder"' makes no sense with Android variants as the same generated models will be used in all variants.
          Use a simple relative path not starting with '..' instead.
          """.trimMargin()
        }
        variant.sourceSets.forEach { sourceProvider ->
          service.addGraphqlDirectory("src/${sourceProvider.name}/graphql/$sourceFolder")
        }
      }
      if (service.outputDirAction == null) {
        service.outputDirAction = Action<Service.OutputDirWire> { wire ->
          AndroidProject.registerGeneratedDirectory(project, variant, wire)
        }
      }
      registerService(service)
    }
  }

  override fun createAllKotlinJvmSourceSetServices(suffix: String, action: Action<Service>) {
    registerDefaultService = false

    KotlinJvmProject.onEachSourceSet(project) { kotlinSourceSet ->
      val name = "${kotlinSourceSet.name}${suffix.capitalize()}"

      val service = project.objects.newInstance(DefaultService::class.java, project.objects, name)
      action.execute(service)

      if (service.graphqlSourceDirectorySet.isReallyEmpty) {
        val sourceFolder = service.sourceFolder.getOrElse(".")
        check(!File(sourceFolder).isRooted && !sourceFolder.startsWith("..")) {
          """ApolloGraphQL: using 'sourceFolder = "$sourceFolder"' makes no sense with Kotlin source sets as the same generated models will be used in all source sets.
          Use a simple relative path not starting with '..' instead.
          """.trimMargin()
        }

        service.addGraphqlDirectory("src/${kotlinSourceSet.name}/graphql/$sourceFolder")
      }
      if (service.outputDirAction == null) {
        service.outputDirAction = Action<Service.OutputDirWire> { wire ->
          kotlinSourceSet.kotlin.srcDir(wire.outputDir)
        }
      }

      registerService(service)
    }
  }


  companion object {
    private const val TASK_GROUP = "apollo"
    const val MIN_GRADLE_VERSION = "5.6"

    private const val USAGE_APOLLO_METADATA = "apollo-metadata"

    private data class Dep(val name: String, val version: String?)

    private fun getDeps(configurations: ConfigurationContainer): List<Dep> {
      return configurations.flatMap { configuration ->
        configuration.incoming.dependencies
            .filter {
              it.group == "com.apollographql.apollo"
            }.map { dependency ->
              Dep(dependency.name, dependency.version)
            }
      }
    }

    // Don't use `graphqlSourceDirectorySet.isEmpty` here, it doesn't work for some reason
    private val SourceDirectorySet.isReallyEmpty
      get() = sourceDirectories.isEmpty

    private fun mainSourceSet(project: Project): String {
      val kotlinExtension = project.extensions.findByName("kotlin")

      return when {
        kotlinExtension is KotlinMultiplatformExtension -> "commonMain"
        else -> "main"
      }
    }
  }
}
