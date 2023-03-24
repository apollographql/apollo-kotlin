package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.OperationIdGenerator
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.Options
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateAsInternal
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.hooks.internal.AddInternalCompilerHooks
import com.apollographql.apollo3.compiler.hooks.internal.ApolloCompilerJavaHooksChain
import com.apollographql.apollo3.compiler.hooks.internal.ApolloCompilerKotlinHooksChain
import com.apollographql.apollo3.gradle.api.AndroidProject
import com.apollographql.apollo3.gradle.api.ApolloAttributes
import com.apollographql.apollo3.gradle.api.ApolloExtension
import com.apollographql.apollo3.gradle.api.Service
import com.apollographql.apollo3.gradle.api.androidExtension
import com.apollographql.apollo3.gradle.api.isKotlinMultiplatform
import com.apollographql.apollo3.gradle.api.javaConvention
import com.apollographql.apollo3.gradle.api.kotlinMultiplatformExtension
import com.apollographql.apollo3.gradle.api.kotlinProjectExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

abstract class DefaultApolloExtension(
    private val project: Project,
    private val defaultService: DefaultService,
) : ApolloExtension, Service by defaultService {

  private var codegenOnGradleSyncConfigured: Boolean = false
  private val services = mutableListOf<DefaultService>()
  private val checkVersionsTask: TaskProvider<Task>
  private val metadataConfiguration: Configuration
  private val schemaConfiguration: Configuration
  private val usedCoordinatesConfiguration: Configuration
  private val rootProvider: TaskProvider<Task>
  private var registerDefaultService = true
  private var adhocComponentWithVariants: AdhocComponentWithVariants? = null

  @get:Inject
  protected abstract val softwareComponentFactory: SoftwareComponentFactory

  // Called when the plugin is applied
  init {
    require(GradleVersion.current() >= GradleVersion.version(MIN_GRADLE_VERSION)) {
      "apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater"
    }

    usedCoordinatesConfiguration = project.configurations.create(ModelNames.usedCoordinatesConfiguration()) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = false

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_USED_COORDINATES))
      }
    }

    metadataConfiguration = project.configurations.create(ModelNames.metadataConfiguration()) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = false

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
      }
    }

    schemaConfiguration = project.configurations.create(ModelNames.schemaConfiguration()) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = false

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_SCHEMA))
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
      task.projectRootDir = project.rootDir.absolutePath
    }

    /**
     * A simple task to be used from the command line to ease the schema upload
     */
    project.tasks.register(ModelNames.pushApolloSchema(), ApolloPushSchemaTask::class.java) { task ->
      task.group = TASK_GROUP
      task.projectRootDir = project.rootDir.absolutePath
    }

    /**
     * A simple task to be used from the command line to ease schema conversion
     */
    project.tasks.register(ModelNames.convertApolloSchema(), ApolloConvertSchemaTask::class.java) { task ->
      task.group = TASK_GROUP
      task.projectRootDir = project.rootDir.absolutePath
    }

    project.afterEvaluate {
      if (registerDefaultService) {
        val packageNameLine = if (defaultService.packageName.isPresent) {
          "packageName.set(\"${defaultService.packageName.get()}\")"
        } else {
          "packageNamesFromFilePaths()"
        }
        it.logger.warn("""
            Apollo: using the default service is deprecated and will be removed in a future version. Please define your service explicitly:
            
            apollo {
              service("service") {
                $packageNameLine
              }
            }
          """.trimIndent())
        registerService(defaultService)
      } else {
        @Suppress("DEPRECATION")
        check(defaultService.graphqlSourceDirectorySet.isEmpty
            && defaultService.schemaFile.isPresent.not()
            && defaultService.schemaFiles.isEmpty
            && defaultService.alwaysGenerateTypesMatching.isPresent.not()
            && defaultService.scalarTypeMapping.isEmpty()
            && defaultService.scalarAdapterMapping.isEmpty()
            && defaultService.customScalarsMapping.isPresent.not()
            && defaultService.customTypeMapping.isPresent.not()
            && defaultService.excludes.isPresent.not()
            && defaultService.includes.isPresent.not()
            && defaultService.failOnWarnings.isPresent.not()
            && defaultService.generateApolloMetadata.isPresent.not()
            && defaultService.generateAsInternal.isPresent.not()
            && defaultService.codegenModels.isPresent.not()
            && defaultService.addTypename.isPresent.not()
            && defaultService.generateFragmentImplementations.isPresent.not()
            && defaultService.requiresOptInAnnotation.isPresent.not()
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

      maybeLinkSqlite()

      checkForLegacyJsTarget()
    }
  }

  private fun checkForLegacyJsTarget() {
    val kotlin = project.extensions.findByName("kotlin") as? KotlinMultiplatformExtension
    val hasLegacyJsTarget = kotlin?.targets?.any { target -> target is KotlinJsTarget && target.irTarget == null } == true
    check(!hasLegacyJsTarget) {
      "Apollo: LEGACY js target is not supported by Apollo, please use IR."
    }
  }

  private fun maybeLinkSqlite() {
    val doLink = when (linkSqlite.orNull) {
      false -> return // explicit opt-out
      true -> true // explicit opt-in
      null -> { // default: automatic detection
        project.configurations.any {
          it.dependencies.any {
            // Try to detect if a native version of apollo-normalized-cache-sqlite is in the classpath
            it.name.contains("apollo-normalized-cache-sqlite")
                && !it.name.contains("jvm")
                && !it.name.contains("android")
          }
        }
      }
    }

    if (doLink) {
      linkSqlite(project)
    }
  }

  /**
   * Call from users to explicitly register a service or by the plugin to register the implicit service
   */
  override fun service(name: String, action: Action<Service>) {
    registerDefaultService = false

    val service = project.objects.newInstance(DefaultService::class.java, project, name)
    action.execute(service)

    registerService(service)

    maybeConfigureCodegenOnGradleSync()
  }

  // See https://twitter.com/Sellmair/status/1619308362881187840
  private fun maybeConfigureCodegenOnGradleSync() {
    if (codegenOnGradleSyncConfigured) {
      return
    }

    codegenOnGradleSyncConfigured = true
    if (this.generateSourcesDuringGradleSync.getOrElse(true)) {
      project.tasks.maybeCreate("prepareKotlinIdeaImport").dependsOn(generateApolloSources)
    }
  }

  // Gradle will consider the task never UP-TO-DATE if we pass a lambda to doLast()
  @Suppress("ObjectLiteralToLambda")
  private fun registerCheckVersionsTask(): TaskProvider<Task> {
    return project.tasks.register(ModelNames.checkApolloVersions()) {
      val outputFile = BuildDirLayout.versionCheck(project)

      it.inputs.property("allVersions", Callable {
        val allDeps = (
            getDeps(project.rootProject.buildscript.configurations) +
                getDeps(project.buildscript.configurations) +
                getDeps(project.configurations)
            )
        allDeps.distinct().sorted()
      })
      it.outputs.file(outputFile)

      it.doLast(object : Action<Task> {
        override fun execute(t: Task) {
          val allVersions = it.inputs.properties["allVersions"] as List<*>

          check(allVersions.size <= 1) {
            "Apollo: All apollo versions should be the same. Found:\n$allVersions"
          }

          val version = allVersions.firstOrNull()
          outputFile.get().asFile.parentFile.mkdirs()
          outputFile.get().asFile.writeText("All versions are consistent: $version")
        }
      })
    }
  }

  private fun createConfiguration(
      name: String,
      isCanBeConsumed: Boolean,
      extendsFrom: Configuration?,
      usage: String,
      serviceName: String,
  ): Configuration {
    return project.configurations.create(name) {
      it.isCanBeConsumed = isCanBeConsumed
      it.isCanBeResolved = !isCanBeConsumed

      if (extendsFrom != null) {
        it.extendsFrom(extendsFrom)
      }

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, usage))
        it.attribute(ApolloAttributes.APOLLO_SERVICE_ATTRIBUTE, project.objects.named(ApolloAttributes.Service::class.java, serviceName))
      }
    }
  }

  private fun registerService(service: DefaultService) {
    check(services.find { it.name == service.name } == null) {
      "There is already a service named ${service.name}, please use another name"
    }
    services.add(service)

    val metadataProducerConfiguration = createConfiguration(
        name = ModelNames.metadataProducerConfiguration(service),
        isCanBeConsumed = true,
        extendsFrom = metadataConfiguration,
        usage = USAGE_APOLLO_METADATA,
        serviceName = service.name,
    )

    val metadataConsumerConfiguration = createConfiguration(
        name = ModelNames.metadataConsumerConfiguration(service),
        isCanBeConsumed = false,
        extendsFrom = metadataConfiguration,
        usage = USAGE_APOLLO_METADATA,
        serviceName = service.name,
    )

    val usedCoordinatesProducerConfiguration = createConfiguration(
        name = ModelNames.usedCoordinatesProducerConfiguration(service),
        isCanBeConsumed = true,
        extendsFrom = usedCoordinatesConfiguration,
        usage = USAGE_APOLLO_USED_COORDINATES,
        serviceName = service.name,
    )

    val usedCoordinatesConsumerConfiguration = createConfiguration(
        name = ModelNames.usedCoordinatesConsumerConfiguration(service),
        isCanBeConsumed = false,
        extendsFrom = usedCoordinatesConfiguration,
        usage = USAGE_APOLLO_USED_COORDINATES,
        serviceName = service.name,
    )

    val schemaProducerConfiguration = createConfiguration(
        name = ModelNames.schemaProducerConfiguration(service),
        isCanBeConsumed = true,
        extendsFrom = null,
        usage = USAGE_APOLLO_SCHEMA,
        serviceName = service.name,
    )

    val schemaConsumerConfiguration = createConfiguration(
        name = ModelNames.schemaConsumerConfiguration(service),
        isCanBeConsumed = false,
        extendsFrom = schemaConfiguration,
        usage = USAGE_APOLLO_SCHEMA,
        serviceName = service.name,
    )

    val usedCoordinatesTaskProvider = registerUsedCoordinatesTask(project, service, schemaConsumerConfiguration)
    if (service.usedCoordinates != null) {
      registerUsedCoordinatesAggregateTask(project, service, usedCoordinatesTaskProvider, usedCoordinatesConsumerConfiguration)
    }
    val codegenTaskProvider = registerCodeGenTask(project, service, metadataConsumerConfiguration, usedCoordinatesConsumerConfiguration)
    val schemaTaskProvider = registerSchemaTask(project, service, schemaConsumerConfiguration)

    project.artifacts {
      it.add(usedCoordinatesProducerConfiguration.name, usedCoordinatesTaskProvider.flatMap { it.outputFile }) {
        it.classifier = service.name
      }
      it.add(schemaProducerConfiguration.name, schemaTaskProvider.flatMap { it.outputFile }) {
        it.classifier = service.name
      }
    }

    project.afterEvaluate {
      if (shouldGenerateMetadata(service)) {
        maybeSetupPublishingForConfiguration(metadataProducerConfiguration)
        project.artifacts {
          it.add(metadataProducerConfiguration.name, codegenTaskProvider.flatMap { it.metadataOutputFile }) {
            it.classifier = service.name
          }
        }
      }
    }

    codegenTaskProvider.configure {
      it.dependsOn(checkVersionsTask)
      it.dependsOn(metadataConsumerConfiguration)
      if (service.usedCoordinates == null) {
        it.dependsOn(usedCoordinatesConsumerConfiguration)
      }
    }

    val checkApolloDuplicates = maybeRegisterCheckDuplicates(project.rootProject, service)

    // Add project dependency on root project to this project, with our new configurations
    project.rootProject.dependencies.apply {
      add(
          ModelNames.duplicatesConsumerConfiguration(service),
          project(mapOf("path" to project.path))
      )
    }

    codegenTaskProvider.configure {
      it.finalizedBy(checkApolloDuplicates)
    }

    if (service.operationOutputAction != null) {
      val operationOutputConnection = Service.OperationOutputConnection(
          task = codegenTaskProvider,
          operationOutputFile = codegenTaskProvider.flatMap { it.operationOutputFile }
      )
      service.operationOutputAction!!.execute(operationOutputConnection)
    }

    if (service.outputDirAction == null) {
      service.outputDirAction = defaultOutputDirAction
    }
    service.outputDirAction!!.execute(
        DefaultDirectoryConnection(
            project = project,
            task = codegenTaskProvider,
            outputDir = codegenTaskProvider.flatMap { it.outputDir }
        )
    )

    @Suppress("DEPRECATION")
    if (service.generateTestBuilders.getOrElse(false)) {
      if (service.testDirAction == null) {
        service.testDirAction = defaultTestDirAction
      }
      service.testDirAction!!.execute(
          DefaultDirectoryConnection(
              project = project,
              task = codegenTaskProvider,
              outputDir = codegenTaskProvider.flatMap { it.testDir }
          )
      )
    }

    rootProvider.configure {
      it.dependsOn(codegenTaskProvider)
    }

    registerDownloadSchemaTasks(service)
    maybeRegisterRegisterOperationsTasks(project, service, codegenTaskProvider)
  }

  private fun registerUsedCoordinatesTask(
      project: Project,
      service: DefaultService,
      schemaConsumerConfiguration: Configuration,
  ): TaskProvider<ApolloGenerateUsedCoordinatesTask> {
    return project.tasks.register(ModelNames.generateApolloUsedCoordinates(service), ApolloGenerateUsedCoordinatesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo used coordinates for ${service.name} GraphQL queries"

      if (service.graphqlSourceDirectorySet.isReallyEmpty) {
        val sourceFolder = service.sourceFolder.getOrElse("")
        val dir = File(project.projectDir, "src/${mainSourceSet(project)}/graphql/$sourceFolder")

        service.graphqlSourceDirectorySet.srcDir(dir)
      }
      service.graphqlSourceDirectorySet.include(service.includes.getOrElse(listOf("**/*.graphql", "**/*.gql")))
      service.graphqlSourceDirectorySet.exclude(service.excludes.getOrElse(emptyList()))

      task.outputFile.apply {
        set(BuildDirLayout.usedCoordinates(project, service))
        disallowChanges()
      }
      task.graphqlFiles.setFrom(service.graphqlSourceDirectorySet)
      // Since this is stored as a list of string, the order matter hence the sorting
      task.rootFolders.set(project.provider { service.graphqlSourceDirectorySet.srcDirs.map { it.relativeTo(project.projectDir).path }.sorted() })
      // This has to be lazy in case the schema is not written yet during configuration
      // See the `graphql files can be generated by another task` test
      task.schemaFiles.from(project.provider { service.lazySchemaFiles(project) })

      task.incomingSchemaFiles.from(schemaConsumerConfiguration)
    }
  }

  private fun registerUsedCoordinatesAggregateTask(
      project: Project,
      service: DefaultService,
      selfUsedCoordinatesTaskProvider: TaskProvider<ApolloGenerateUsedCoordinatesTask>,
      incomingUsedCoordinates: Configuration,
  ): TaskProvider<ApolloGenerateUsedCoordinatesAggregateTask> {
    return project.tasks.register(ModelNames.generateApolloUsedCoordinatesAggregate(service), ApolloGenerateUsedCoordinatesAggregateTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo all used coordinates for ${service.name} GraphQL queries"

      task.outputFile.set(service.usedCoordinates!!)
      task.incomingUsedCoordinates.from(incomingUsedCoordinates)
      task.incomingUsedCoordinates.from(selfUsedCoordinatesTaskProvider.flatMap { it.outputFile })
    }
  }

  private fun registerSchemaTask(
      project: Project,
      service: DefaultService,
      schemaConsumerConfiguration: Configuration,
  ): TaskProvider<ApolloGenerateSchemaTask> {
    return project.tasks.register(ModelNames.generateApolloSchema(service), ApolloGenerateSchemaTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo schema for ${service.name}"

      task.outputFile.apply {
        set(BuildDirLayout.schema(project, service))
        disallowChanges()
      }
      // Since this is stored as a list of string, the order matter hence the sorting
      task.rootFolders.set(project.provider { service.graphqlSourceDirectorySet.srcDirs.map { it.relativeTo(project.projectDir).path }.sorted() })
      // This has to be lazy in case the schema is not written yet during configuration
      // See the `graphql files can be generated by another task` test
      task.schemaFiles.from(project.provider { service.lazySchemaFiles(project) })

      task.incomingSchemaFiles.from(schemaConsumerConfiguration)
    }
  }

  private fun maybeSetupPublishingForConfiguration(producerConfiguration: Configuration) {
    val publishing = project.extensions.findByType(PublishingExtension::class.java) ?: return

    if (adhocComponentWithVariants == null) {
      adhocComponentWithVariants = softwareComponentFactory.adhoc("apollo")

      publishing.publications.create("apollo", MavenPublication::class.java) {
        it.from(adhocComponentWithVariants)
        it.artifactId = "${project.name}-apollo"
      }
    }
    adhocComponentWithVariants!!.addVariantsFromConfiguration(producerConfiguration) {}
  }

  private fun maybeRegisterRegisterOperationsTasks(
      project: Project,
      service: DefaultService,
      codegenProvider: TaskProvider<ApolloGenerateSourcesTask>,
  ) {
    val registerOperationsConfig = service.registerOperationsConfig
    if (registerOperationsConfig != null) {
      project.tasks.register(ModelNames.registerApolloOperations(service), ApolloRegisterOperationsTask::class.java) { task ->
        task.group = TASK_GROUP

        task.graph.set(registerOperationsConfig.graph)
        task.graphVariant.set(registerOperationsConfig.graphVariant)
        task.key.set(registerOperationsConfig.key)
        task.operationOutput.set(codegenProvider.flatMap { it.operationOutputFile })
      }
    }
  }

  /**
   * Generate metadata
   * - if the user opted in
   * - or if this project belongs to a multi-module build
   * The last case is needed to check for potential duplicate types
   */
  private fun shouldGenerateMetadata(service: DefaultService): Boolean {
    return service.generateApolloMetadata.getOrElse(false)
        || metadataConfiguration.dependencies.isNotEmpty()
  }

  /**
   * The default wiring.
   */
  private val defaultOutputDirAction = Action<Service.DirectoryConnection> { connection ->
    when {
      project.kotlinMultiplatformExtension != null -> {
        connection.connectToKotlinSourceSet("commonMain")
      }

      project.androidExtension != null -> {
        if (registerDefaultService) {
          // The default service is created from `afterEvaluate` and it looks like it's too late to register new sources
          connection.connectToAndroidSourceSet("main")
        } else {
          connection.connectToAllAndroidVariants()
        }
      }

      project.kotlinProjectExtension != null -> {
        connection.connectToKotlinSourceSet("main")
      }

      project.javaConvention != null -> {
        connection.connectToJavaSourceSet("main")
      }

      else -> throw IllegalStateException("Cannot find a Java/Kotlin extension, please apply the kotlin or java plugin")
    }
  }

  private val defaultTestDirAction = Action<Service.DirectoryConnection> { connection ->
    when {
      project.kotlinMultiplatformExtension != null -> {
        connection.connectToKotlinSourceSet("commonTest")
      }

      project.androidExtension != null -> {
        connection.connectToAndroidSourceSet("test")
        connection.connectToAndroidSourceSet("androidTest")
      }

      project.kotlinProjectExtension != null -> {
        connection.connectToKotlinSourceSet("test")
      }

      project.javaConvention != null -> {
        connection.connectToJavaSourceSet("test")
      }

      else -> throw IllegalStateException("Cannot find a Java/Kotlin extension, please apply the kotlin or java plugin")
    }
  }

  private fun maybeRegisterCheckDuplicates(rootProject: Project, service: Service): TaskProvider<ApolloCheckDuplicatesTask> {
    val taskName = ModelNames.checkApolloDuplicates(service)
    return try {
      @Suppress("UNCHECKED_CAST")
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

  private fun Project.hasJavaPlugin() = project.extensions.findByName("java") != null
  private fun Project.hasKotlinPlugin() = project.extensions.findByName("kotlin") != null

  private fun registerCodeGenTask(
      project: Project,
      service: DefaultService,
      metadataConsumerConfiguration: Configuration,
      usedCoordinatesConsumerConfiguration: Configuration,
  ): TaskProvider<ApolloGenerateSourcesTask> {
    return project.tasks.register(ModelNames.generateApolloSources(service), ApolloGenerateSourcesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo models for ${service.name} GraphQL queries"


      if (service.graphqlSourceDirectorySet.isReallyEmpty) {
        val sourceFolder = service.sourceFolder.getOrElse("")
        val dir = File(project.projectDir, "src/${mainSourceSet(project)}/graphql/$sourceFolder")

        service.graphqlSourceDirectorySet.srcDir(dir)
      }
      service.graphqlSourceDirectorySet.include(service.includes.getOrElse(listOf("**/*.graphql", "**/*.gql")))
      service.graphqlSourceDirectorySet.exclude(service.excludes.getOrElse(emptyList()))

      task.graphqlFiles.setFrom(service.graphqlSourceDirectorySet)
      // Since this is stored as a list of string, the order matter hence the sorting
      task.rootFolders.set(project.provider { service.graphqlSourceDirectorySet.srcDirs.map { it.relativeTo(project.projectDir).path }.sorted() })
      // This has to be lazy in case the schema is not written yet during configuration
      // See the `graphql files can be generated by another task` test
      task.schemaFiles.from(project.provider { service.lazySchemaFiles(project) })

      task.operationOutputGenerator = service.operationOutputGenerator.getOrElse(
          OperationOutputGenerator.Default(
              service.operationIdGenerator.orElse(OperationIdGenerator.Sha256).get()
          )
      )

      if (project.hasKotlinPlugin()) {
        checkKotlinPluginVersion(project)
      }

      val generateKotlinModels: Boolean
      when {
        service.generateKotlinModels.isPresent -> {
          generateKotlinModels = service.generateKotlinModels.get()
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

      val targetLanguage = if (generateKotlinModels) {
        getKotlinTargetLanguage(service.languageVersion.orNull)
      } else {
        TargetLanguage.JAVA
      }

      task.useSemanticNaming.set(service.useSemanticNaming)
      task.targetLanguage.set(targetLanguage)
      task.warnOnDeprecatedUsages.set(service.warnOnDeprecatedUsages)
      task.failOnWarnings.set(service.failOnWarnings)

      val scalarTypeMappingFallbackOldSyntax = service.customScalarsMapping.orElse(
          service.customTypeMapping
      ).getOrElse(emptyMap())

      check(service.scalarTypeMapping.isEmpty() || scalarTypeMappingFallbackOldSyntax.isEmpty()) {
        "Apollo: either mapScalar() or customScalarsMapping can be used, but not both"
      }
      @Suppress("DEPRECATION")
      task.scalarTypeMapping.set(
          service.scalarTypeMapping.ifEmpty { scalarTypeMappingFallbackOldSyntax }
      )
      task.scalarAdapterMapping.set(service.scalarAdapterMapping)
      task.outputDir.apply {
        set(service.outputDir.orElse(BuildDirLayout.outputDir(project, service)).get())
        disallowChanges()
      }
      task.testDir.apply {
        set(service.testDir.orElse(BuildDirLayout.testDir(project, service)).get())
        disallowChanges()
      }
      task.debugDir.apply {
        set(service.debugDir)
        disallowChanges()
      }
      if (service.generateOperationOutput.getOrElse(false)) {
        task.operationOutputFile.apply {
          set(service.operationOutputFile.orElse(BuildDirLayout.operationOutput(project, service)))
          disallowChanges()
        }
      }
      if (shouldGenerateMetadata(service)) {
        task.metadataOutputFile.apply {
          set(BuildDirLayout.metadata(project, service))
          disallowChanges()
        }
      }

      task.metadataFiles.from(metadataConsumerConfiguration)

      check(!(service.packageName.isPresent && service.packageNameGenerator.isPresent)) {
        "Apollo: it is an error to specify both 'packageName' and 'packageNameGenerator' " +
            "(either directly or indirectly through useVersion2Compat())"
      }
      var packageNameGenerator = service.packageNameGenerator.orNull
      if (packageNameGenerator == null) {
        packageNameGenerator = PackageNameGenerator.Flat(service.packageName.orNull ?: error("""
            |Apollo: specify 'packageName':
            |apollo {
            |  service("service") {
            |    packageName.set("com.example")
            |  
            |    // Alternatively, if you're migrating from 2.x, you can keep the 2.x   
            |    // behaviour with `packageNamesFromFilePaths()`: 
            |    packageNamesFromFilePaths()
            |  }
            |}
          """.trimMargin()))
      }
      task.packageNameGenerator = packageNameGenerator
      task.generateFilterNotNull.set(project.isKotlinMultiplatform)
      task.usedCoordinates.from(usedCoordinatesConsumerConfiguration)
      if (service.usedCoordinates != null) {
        task.usedCoordinates.from(service.usedCoordinates)
      }
      task.alwaysGenerateTypesMatching.set(service.alwaysGenerateTypesMatching)
      task.projectPath.set(project.path)
      task.generateFragmentImplementations.set(service.generateFragmentImplementations)
      task.generateQueryDocument.set(service.generateQueryDocument)
      task.generateSchema.set(service.generateSchema)
      task.generatedSchemaName.set(service.generatedSchemaName)
      task.generateModelBuilders.set(service.generateModelBuilders.orElse(service.generateModelBuilder))
      task.codegenModels.set(service.codegenModels)
      task.addTypename.set(service.addTypename)
      task.flattenModels.set(service.flattenModels)
      @Suppress("DEPRECATION")
      task.generateTestBuilders.set(service.generateTestBuilders)
      task.generateDataBuilders.set(service.generateDataBuilders)
      task.useSchemaPackageNameForFragments.set(service.useSchemaPackageNameForFragments)
      task.addJvmOverloads.set(service.addJvmOverloads)
      task.sealedClassesForEnumsMatching.set(service.sealedClassesForEnumsMatching)
      task.classesForEnumsMatching.set(service.classesForEnumsMatching)
      task.generateOptionalOperationVariables.set(service.generateOptionalOperationVariables)
      task.languageVersion.set(service.languageVersion)
      task.requiresOptInAnnotation.set(service.requiresOptInAnnotation)
      task.fieldsOnDisjointTypesMustMerge.set(service.fieldsOnDisjointTypesMustMerge)
      task.generatePrimitiveTypes.set(service.generatePrimitiveTypes)
      val nullableFieldStyle: String? = service.nullableFieldStyle.orNull
      task.nullableFieldStyle.set(if (nullableFieldStyle == null) Options.defaultNullableFieldStyle else JavaNullable.fromName(nullableFieldStyle)
          ?: error("Apollo: unknown value '$nullableFieldStyle' for nullableFieldStyle"))
      task.decapitalizeFields.set(service.decapitalizeFields)
      val compilerKotlinHooks = service.compilerKotlinHooks.orNull ?: emptyList()
      val generateAsInternal = service.generateAsInternal.getOrElse(defaultGenerateAsInternal)
      task.compilerKotlinHooks = if (compilerKotlinHooks.isEmpty()) {
        if (generateAsInternal) {
          AddInternalCompilerHooks(setOf(".*"))
        } else {
          ApolloCompilerKotlinHooks.Identity
        }
      } else {
        checkExternalPlugin()
        if (generateAsInternal) {
          ApolloCompilerKotlinHooksChain(compilerKotlinHooks + AddInternalCompilerHooks(setOf(".*")))
        } else {
          ApolloCompilerKotlinHooksChain(compilerKotlinHooks)
        }
      }
      val compilerJavaHooks = service.compilerJavaHooks.orNull ?: emptyList()
      task.compilerJavaHooks = if (compilerJavaHooks.isEmpty()) {
        ApolloCompilerJavaHooks.Identity
      } else {
        checkExternalPlugin()
        ApolloCompilerJavaHooksChain(compilerJavaHooks)
      }
    }
  }

  private fun checkExternalPlugin() {
    check(project.plugins.hasPlugin("com.apollographql.apollo3.external")) {
      "Apollo: to use compilerJavaHooks or compilerKotlinHooks, you need to apply the 'com.apollographql.apollo3.external' Gradle plugin instead of 'com.apollographql.apollo3'"
    }
  }

  /**
   * XXX: this returns an absolute path, which might be an issue for the build cache.
   * I don't think this is much of an issue because tasks like ApolloDownloadSchemaTask don't have any
   * outputs and are therefore never up-to-date so the build cache will not help much.
   *
   * If that ever becomes an issue, making the path relative to the project root might be a good idea.
   */
  private fun lazySchemaFileForDownload(service: DefaultService, schemaFile: RegularFileProperty): String {
    if (schemaFile.isPresent) {
      return schemaFile.get().asFile.absolutePath
    }

    val candidates = service.lazySchemaFiles(project)
    check(candidates.isNotEmpty()) {
      "No schema files found. Specify introspection.schemaFile or registry.schemaFile"
    }
    check(candidates.size == 1) {
      "Multiple schema files found:\n${candidates.joinToString("\n")}\n\nSpecify introspection.schemaFile or registry.schemaFile"
    }

    return candidates.single().absolutePath
  }

  private fun registerDownloadSchemaTasks(service: DefaultService) {
    val introspection = service.introspection
    if (introspection != null) {
      project.tasks.register(ModelNames.downloadApolloSchemaIntrospection(service), ApolloDownloadSchemaTask::class.java) { task ->

        task.group = TASK_GROUP
        task.projectRootDir = project.rootDir.absolutePath
        task.endpoint.set(introspection.endpointUrl)
        task.header = introspection.headers.get().map { "${it.key}: ${it.value}" }
        task.schema.set(project.provider { lazySchemaFileForDownload(service, introspection.schemaFile) })
      }
    }
    val registry = service.registry
    if (registry != null) {
      project.tasks.register(ModelNames.downloadApolloSchemaRegistry(service), ApolloDownloadSchemaTask::class.java) { task ->

        task.group = TASK_GROUP
        task.projectRootDir = project.rootDir.absolutePath
        task.graph.set(registry.graph)
        task.key.set(registry.key)
        task.graphVariant.set(registry.graphVariant)
        task.schema.set(project.provider { lazySchemaFileForDownload(service, registry.schemaFile) })
      }
    }
  }

  override fun createAllAndroidVariantServices(
      sourceFolder: String,
      nameSuffix: String,
      action: Action<Service>,
  ) {
    /**
     * The android plugin will call us back when the variants are ready but before `afterEvaluate`,
     * disable the default service
     */
    registerDefaultService = false

    check(!File(sourceFolder).isRooted && !sourceFolder.startsWith("../..")) {
      """
          Apollo: using 'sourceFolder = "$sourceFolder"' makes no sense with Android variants as the same generated models will be used in all variants.
          """.trimIndent()
    }

    AndroidProject.onEachVariant(project, true) { variant ->
      val name = "${variant.name}${nameSuffix.capitalizeFirstLetter()}"

      service(name) { service ->
        action.execute(service)

        check(!service.sourceFolder.isPresent) {
          "Apollo: service.sourceFolder is not used when calling createAllAndroidVariantServices. Use the parameter instead"
        }
        variant.sourceSets.forEach { sourceProvider ->
          service.srcDir("src/${sourceProvider.name}/graphql/$sourceFolder")
        }
        (service as DefaultService).outputDirAction = Action<Service.DirectoryConnection> { connection ->
          connection.connectToAndroidVariant(variant)
        }
      }
    }
  }

  override fun createAllKotlinSourceSetServices(sourceFolder: String, nameSuffix: String, action: Action<Service>) {
    registerDefaultService = false

    check(!File(sourceFolder).isRooted && !sourceFolder.startsWith("../..")) {
      """Apollo: using 'sourceFolder = "$sourceFolder"' makes no sense with Kotlin source sets as the same generated models will be used in all source sets.
          """.trimMargin()
    }

    createAllKotlinSourceSetServices(this, project, sourceFolder, nameSuffix, action)
  }

  abstract override val linkSqlite: Property<Boolean>
  abstract override val generateSourcesDuringGradleSync: Property<Boolean>

  companion object {
    private const val TASK_GROUP = "apollo"
    const val MIN_GRADLE_VERSION = "5.6"

    private const val USAGE_APOLLO_METADATA = "apollo-metadata"
    private const val USAGE_APOLLO_USED_COORDINATES = "apollo-used-coordinates"
    private const val USAGE_APOLLO_SCHEMA = "apollo-schema"


    private fun getDeps(configurations: ConfigurationContainer): List<String> {
      return configurations.flatMap { configuration ->
        configuration.dependencies
            .filter {
              // the "_" check is for refreshVersions,
              // see https://github.com/jmfayard/refreshVersions/issues/507
              it.group == "com.apollographql.apollo3" && it.version != "_"
            }.map { dependency ->
              dependency.version
            }.filterNotNull()
      }
    }

    // Don't use `graphqlSourceDirectorySet.isEmpty` here, it doesn't work for some reason
    private val SourceDirectorySet.isReallyEmpty
      get() = sourceDirectories.isEmpty

    private fun mainSourceSet(project: Project): String {
      val kotlinExtension = project.extensions.findByName("kotlin")

      return when (kotlinExtension) {
        is KotlinMultiplatformExtension -> "commonMain"
        else -> "main"
      }
    }

    fun DefaultService.lazySchemaFiles(project: Project): Set<File> {
      val files = if (schemaFile.isPresent) {
        check(schemaFiles.isEmpty) {
          "Specifying both schemaFile and schemaFiles is an error"
        }
        project.files(schemaFile)
      } else {
        schemaFiles
      }

      if (!files.isEmpty) {
        return files.files
      }

      return graphqlSourceDirectorySet.srcDirs.flatMap { srcDir ->
        srcDir.walkTopDown().filter {
          it.extension in listOf("json", "sdl", "graphqls")
              && !it.name.startsWith("used") // Avoid detecting the used coordinates as a schema
        }.toList()
      }.toSet()
    }
  }

  private fun Map<String, String>.asScalarInfoMapping(): Map<String, ScalarInfo> =
      mapValues { (_, value) -> ScalarInfo(value, RuntimeAdapterInitializer) }
}
