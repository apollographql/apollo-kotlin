package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.OperationIdGenerator
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.defaultGenerateAsInternal
import com.apollographql.apollo3.compiler.defaultNullableFieldStyle
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.hooks.internal.AddInternalCompilerHooks
import com.apollographql.apollo3.compiler.hooks.internal.ApolloCompilerJavaHooksChain
import com.apollographql.apollo3.compiler.hooks.internal.ApolloCompilerKotlinHooksChain
import com.apollographql.apollo3.gradle.api.AndroidProject
import com.apollographql.apollo3.gradle.api.ApolloAttributes
import com.apollographql.apollo3.gradle.api.ApolloExtension
import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import com.apollographql.apollo3.gradle.api.SchemaConnection
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
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

@Suppress("UnstableApiUsage")
abstract class DefaultApolloExtension(
    private val project: Project,
    private val defaultService: DefaultService,
) : ApolloExtension, Service by defaultService {

  private var codegenOnGradleSyncConfigured: Boolean = false
  private val services = mutableListOf<DefaultService>()
  private val checkVersionsTask: TaskProvider<Task>
  private val generateApolloSources: TaskProvider<Task>
  private var hasExplicitService = false
  private val adhocComponentWithVariants: AdhocComponentWithVariants
  private val apolloMetadataConfiguration: Configuration

  internal fun getServiceInfos(project: Project): List<ApolloGradleToolingModel.ServiceInfo> = services.map { service ->
    DefaultServiceInfo(
        name = service.name,
        schemaFiles = service.lazySchemaFiles(project),
        graphqlSrcDirs = service.graphqlSourceDirectorySet.srcDirs,
        upstreamProjects = service.upstreamDependencies.filterIsInstance<ProjectDependency>().map { it.name }.toSet()
    )
  }

  @get:Inject
  protected abstract val softwareComponentFactory: SoftwareComponentFactory

  // Called when the plugin is applied
  init {
    require(GradleVersion.current() >= GradleVersion.version(MIN_GRADLE_VERSION)) {
      "apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater"
    }

    adhocComponentWithVariants = softwareComponentFactory.adhoc("apollo")
    project.components.add(adhocComponentWithVariants)

    checkVersionsTask = registerCheckVersionsTask()

    /**
     * An aggregate task to easily generate all models
     */
    generateApolloSources = project.tasks.register(ModelNames.generateApolloSources()) {
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

    apolloMetadataConfiguration = project.configurations.create(ModelNames.metadataConfiguration()) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = false
    }

    project.afterEvaluate {
      val hasApolloBlock = !defaultService.graphqlSourceDirectorySet.isEmpty
          || defaultService.schemaFile.isPresent
          || !defaultService.schemaFiles.isEmpty
          || defaultService.alwaysGenerateTypesMatching.isPresent
          || defaultService.scalarTypeMapping.isNotEmpty()
          || defaultService.scalarAdapterMapping.isNotEmpty()
          || defaultService.excludes.isPresent
          || defaultService.includes.isPresent
          || defaultService.failOnWarnings.isPresent
          || defaultService.generateApolloMetadata.isPresent
          || defaultService.generateAsInternal.isPresent
          || defaultService.codegenModels.isPresent
          || defaultService.addTypename.isPresent
          || defaultService.generateFragmentImplementations.isPresent
          || defaultService.requiresOptInAnnotation.isPresent
          || defaultService.packageName.isPresent
          || defaultService.packageNameGenerator.isPresent

      if (hasApolloBlock) {
        val packageNameLine = if (defaultService.packageName.isPresent) {
          "packageName.set(\"${defaultService.packageName.get()}\")"
        } else {
          "packageNamesFromFilePaths()"
        }
        error("""
            Apollo: using the default service is not supported anymore. Please define your service explicitly:
            
            apollo {
              service("service") {
                $packageNameLine
              }
            }
          """.trimIndent())
      }

      maybeLinkSqlite()
      checkForLegacyJsTarget()
      checkApolloMetadataIsEmpty()
    }
  }

  private fun checkApolloMetadataIsEmpty() {
    check(apolloMetadataConfiguration.dependencies.isEmpty()) {
      val projectLines = apolloMetadataConfiguration.dependencies.map {
        if (it is ProjectDependency) {
          "project(\"${it.dependencyProject.path}\")"
        } else if (it is ExternalModuleDependency) {
          "\"group:artifact:version\""
        } else {
          "project(\":foo\")"
        }
      }.joinToString("\n") { "dependsOn($it)" }
      """
        Apollo: using apolloMetadata is not supported anymore. Please use `dependsOn`:
         
        apollo {
          service("service") {
            $projectLines
          }
        }
      """.trimIndent()
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
    hasExplicitService = false

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

    if (service.graphqlSourceDirectorySet.isReallyEmpty) {
      val sourceFolder = service.sourceFolder.getOrElse("")
      val dir = File(project.projectDir, "src/${mainSourceSet(project)}/graphql/$sourceFolder")

      service.graphqlSourceDirectorySet.srcDir(dir)
    }
    service.graphqlSourceDirectorySet.include(service.includes.getOrElse(listOf("**/*.graphql", "**/*.gql")))
    service.graphqlSourceDirectorySet.exclude(service.excludes.getOrElse(emptyList()))

    val operationOutputConnection: Service.OperationOutputConnection
    val directoryConnection: Service.DirectoryConnection

    if (service.upstreamDependencies.isNotEmpty() || service.generateApolloMetadata.getOrElse(false)) {
      val schemaConsumerConfiguration = createConfiguration(
          name = ModelNames.schemaConsumerConfiguration(service),
          isCanBeConsumed = false,
          extendsFrom = null,
          usage = USAGE_APOLLO_SCHEMA,
          serviceName = service.name,
      )

      val schemaProducerConfiguration = createConfiguration(
          name = ModelNames.schemaProducerConfiguration(service),
          isCanBeConsumed = true,
          extendsFrom = schemaConsumerConfiguration,
          usage = USAGE_APOLLO_SCHEMA,
          serviceName = service.name,
      )

      val upstreamIrConsumerConfiguration = createConfiguration(
          name = ModelNames.upstreamIrConsumerConfiguration(service),
          isCanBeConsumed = false,
          extendsFrom = null,
          usage = USAGE_APOLLO_IR,
          serviceName = service.name,
      )

      val upstreamIrProducerConfiguration = createConfiguration(
          name = ModelNames.upstreamIrProducerConfiguration(service),
          isCanBeConsumed = true,
          extendsFrom = upstreamIrConsumerConfiguration,
          usage = USAGE_APOLLO_IR,
          serviceName = service.name,
      )

      val downstreamIrConsumerConfiguration = createConfiguration(
          name = ModelNames.downstreamIrConsumerConfiguration(service),
          isCanBeConsumed = false,
          extendsFrom = null,
          usage = USAGE_APOLLO_DOWNSTREAM_IR,
          serviceName = service.name,
      )

      val downstreamIrProducerConfiguration = createConfiguration(
          name = ModelNames.downstreamIrProducerConfiguration(service),
          isCanBeConsumed = true,
          extendsFrom = downstreamIrConsumerConfiguration,
          usage = USAGE_APOLLO_DOWNSTREAM_IR,
          serviceName = service.name,
      )

      val metadataConsumerConfiguration = createConfiguration(
          name = ModelNames.metadataConsumerConfiguration(service),
          isCanBeConsumed = false,
          extendsFrom = null,
          usage = USAGE_APOLLO_METADATA,
          serviceName = service.name,
      )

      val metadataProducerConfiguration = createConfiguration(
          name = ModelNames.metadataProducerConfiguration(service),
          isCanBeConsumed = true,
          extendsFrom = metadataConsumerConfiguration,
          usage = USAGE_APOLLO_METADATA,
          serviceName = service.name,
      )

      val schemaTaskProvider = registerSchemaTask(project, service, schemaConsumerConfiguration)
      val irTaskProvider = registerIrTask(project, service, schemaConsumerConfiguration, schemaTaskProvider, upstreamIrConsumerConfiguration)
      val usedCoordinatesTaskProvider = if (service.upstreamDependencies.isEmpty()) {
        registerUsedCoordinatesTask(project, service, downstreamIrConsumerConfiguration, irTaskProvider)
      } else {
        null
      }
      val codegenTaskProvider = registerCodegenFromIrTask(project, service, schemaConsumerConfiguration, schemaTaskProvider, usedCoordinatesTaskProvider, irTaskProvider, metadataConsumerConfiguration)

      operationOutputConnection = Service.OperationOutputConnection(
          task = codegenTaskProvider,
          operationOutputFile = codegenTaskProvider.flatMap { it.operationOutputFile }
      )

      directoryConnection = DefaultDirectoryConnection(
          project = project,
          task = codegenTaskProvider,
          outputDir = codegenTaskProvider.flatMap { it.outputDir }
      )

      project.artifacts {
        it.add(schemaProducerConfiguration.name, schemaTaskProvider.flatMap { it.outputFile }) {
          it.classifier = "apollo-schema-${service.name}"
        }
        it.add(upstreamIrProducerConfiguration.name, irTaskProvider.flatMap { it.outputFile }) {
          it.classifier = "apollo-ir-${service.name}"
        }
        it.add(downstreamIrProducerConfiguration.name, irTaskProvider.flatMap { it.outputFile }) {
          it.classifier = "apollo-ir-${service.name}"
        }
        it.add(metadataProducerConfiguration.name, codegenTaskProvider.flatMap { it.metadataOutputFile }) {
          it.classifier = "apollo-metadata-${service.name}"
        }
      }

      adhocComponentWithVariants.addVariantsFromConfiguration(metadataProducerConfiguration) {}
      adhocComponentWithVariants.addVariantsFromConfiguration(upstreamIrProducerConfiguration) {}
      adhocComponentWithVariants.addVariantsFromConfiguration(schemaProducerConfiguration) {}

      service.upstreamDependencies.forEach {
        project.dependencies.add(schemaConsumerConfiguration.name, it)
        project.dependencies.add(upstreamIrConsumerConfiguration.name, it)
        project.dependencies.add(metadataConsumerConfiguration.name, it)
      }
      service.downstreamDependencies.forEach {
        project.dependencies.add(downstreamIrConsumerConfiguration.name, it)
      }
    } else {
      val codegenTaskProvider = registerCodeGenTask(project, service)

      operationOutputConnection = Service.OperationOutputConnection(
          task = codegenTaskProvider,
          operationOutputFile = codegenTaskProvider.flatMap { it.operationOutputFile }
      )

      directoryConnection = DefaultDirectoryConnection(
          project = project,
          task = codegenTaskProvider,
          outputDir = codegenTaskProvider.flatMap { it.outputDir }
      )
    }

    if (project.hasKotlinPlugin()) {
      checkKotlinPluginVersion(project)
    }

    if (service.operationOutputAction != null) {
      service.operationOutputAction!!.execute(operationOutputConnection)
    }
    maybeRegisterRegisterOperationsTasks(project, service, operationOutputConnection)

    if (service.outputDirAction == null) {
      service.outputDirAction = defaultOutputDirAction
    }
    service.outputDirAction!!.execute(directoryConnection)

    directoryConnection.task.configure {
      it.dependsOn(checkVersionsTask)
    }
    generateApolloSources.configure {
      it.dependsOn(directoryConnection.task)
    }

    registerDownloadSchemaTasks(service)

    service.generateApolloMetadata.disallowChanges()
    service.registered = true
  }

  private fun registerCodegenFromIrTask(
      project: Project,
      service: DefaultService,
      schemaConsumerConfiguration: Configuration,
      schemaTaskProvider: TaskProvider<ApolloGenerateSchemaTask>,
      usedCoordinatesTaskProvider: TaskProvider<ApolloGenerateUsedCoordinatesAndCheckFragmentsTask>?,
      irTaskProvider: TaskProvider<ApolloGenerateIrTask>,
      upstreamMetadata: Configuration,
  ): TaskProvider<ApolloGenerateSourcesFromIrTask> {
    return project.tasks.register(ModelNames.generateApolloSourcesFromIr(service), ApolloGenerateSourcesFromIrTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo models for ${service.name} GraphQL queries"

      task.codegenSchemas.from(schemaConsumerConfiguration)
      task.codegenSchemas.from(schemaTaskProvider.flatMap { it.outputFile })
      task.irOperations.set(irTaskProvider.flatMap { it.outputFile })
      task.upstreamMetadata.from(upstreamMetadata)
      if (usedCoordinatesTaskProvider != null) {
        task.usedCoordinates.set(usedCoordinatesTaskProvider.flatMap { it.outputFile })
      }
      task.metadataOutputFile.set(BuildDirLayout.metadata(project, service))

      configureBaseCodegenTask(project, task, service)
    }
  }

  private fun registerUsedCoordinatesTask(
      project: Project,
      service: DefaultService,
      downstreamIrOperations: Configuration,
      irTaskProvider: TaskProvider<ApolloGenerateIrTask>,
  ): TaskProvider<ApolloGenerateUsedCoordinatesAndCheckFragmentsTask> {
    return project.tasks.register(ModelNames.generateApolloUsedCoordinates(service), ApolloGenerateUsedCoordinatesAndCheckFragmentsTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo Used Coordinates for ${service.name} GraphQL queries"

      task.outputFile.set(BuildDirLayout.usedCoordinates(project, service))
      task.downStreamIrOperations.from(downstreamIrOperations)
      task.irOperations.set(irTaskProvider.flatMap { it.outputFile })
    }
  }

  private fun registerIrTask(
      project: Project,
      service: DefaultService,
      schemaConsumerConfiguration: Configuration,
      schemaTaskProvider: TaskProvider<ApolloGenerateSchemaTask>,
      upstreamIrFiles: Configuration,
  ): TaskProvider<ApolloGenerateIrTask> {
    return project.tasks.register(ModelNames.generateApolloIr(service), ApolloGenerateIrTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo IR for ${service.name} GraphQL queries"

      task.graphqlFiles.setFrom(service.graphqlSourceDirectorySet)
      task.codegenSchemas.from(schemaConsumerConfiguration)
      task.codegenSchemas.from(schemaTaskProvider.flatMap { it.outputFile })
      task.upstreamIrFiles.from(upstreamIrFiles)
      task.addTypename.set(service.addTypename)
      task.fieldsOnDisjointTypesMustMerge.set(service.fieldsOnDisjointTypesMustMerge)
      task.decapitalizeFields.set(service.decapitalizeFields)
      task.flattenModels.set(service.flattenModels())
      task.warnOnDeprecatedUsages.set(service.warnOnDeprecatedUsages)
      task.failOnWarnings.set(service.failOnWarnings)
      task.generateOptionalOperationVariables.set(service.generateOptionalOperationVariables)
      task.alwaysGenerateTypesMatching.set(service.alwaysGenerateTypesMatching())

      task.outputFile.set(BuildDirLayout.ir(project, service))
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

      task.outputFile.set(BuildDirLayout.schema(project, service))
      // This has to be lazy in case the schema is not written yet during configuration
      // See the `graphql files can be generated by another task` test
      task.schemaFiles.from(project.provider { service.lazySchemaFiles(project) })

      task.scalarTypeMapping.set(service.scalarTypeMapping)
      task.scalarAdapterMapping.set(service.scalarAdapterMapping)
      task.packageNameGenerator = service.packageNameGenerator()
      task.codegenModels.set(service.codegenModels())
      task.targetLanguage.set(service.targetLanguage())
      task.userGenerateKotlinModels.set(service.generateKotlinModels)
      task.userCodegenModels.set(service.codegenModels)
      task.generateDataBuilders.set(service.generateDataBuilders)

      task.upstreamSchemaFiles.from(schemaConsumerConfiguration)
    }
  }

  private fun maybeRegisterRegisterOperationsTasks(
      project: Project,
      service: DefaultService,
      operationOutputConnection: Service.OperationOutputConnection,
  ) {
    val registerOperationsConfig = service.registerOperationsConfig
    if (registerOperationsConfig != null) {
      project.tasks.register(ModelNames.registerApolloOperations(service), ApolloRegisterOperationsTask::class.java) { task ->
        task.group = TASK_GROUP

        task.graph.set(registerOperationsConfig.graph)
        task.graphVariant.set(registerOperationsConfig.graphVariant)
        task.key.set(registerOperationsConfig.key)
        task.operationOutput.set(operationOutputConnection.operationOutputFile)
      }
    }
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
        // The default service is created from `afterEvaluate` and it looks like it's too late to register new sources
        connection.connectToAndroidSourceSet("main")
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

  private fun configureBaseCodegenTask(
      project: Project,
      task: ApolloGenerateSourcesBase,
      service: DefaultService,
  ) {
    task.operationOutputGenerator = service.operationOutputGenerator.getOrElse(
        OperationOutputGenerator.Default(
            service.operationIdGenerator.orElse(OperationIdGenerator.Sha256).get()
        )
    )
    service.operationOutputGenerator.disallowChanges()

    task.useSemanticNaming.set(service.useSemanticNaming)
    task.outputDir.set(service.outputDir.orElse(BuildDirLayout.outputDir(project, service)))

    if (service.generateOperationOutput.getOrElse(false)) {
      task.operationOutputFile.set(service.operationOutputFile.orElse(BuildDirLayout.operationOutput(project, service)))
    }
    service.generateOperationOutput.disallowChanges()

    task.packageNameGenerator = service.packageNameGenerator()
    service.packageNameGenerator.disallowChanges()
    service.packageName.disallowChanges()

    task.generateFilterNotNull.set(project.isKotlinMultiplatform)
    task.generateFragmentImplementations.set(service.generateFragmentImplementations)
    task.generateQueryDocument.set(service.generateQueryDocument)
    task.generateSchema.set(service.generateSchema)
    task.generatedSchemaName.set(service.generatedSchemaName)
    task.generateModelBuilders.set(service.generateModelBuilders)
    task.addJvmOverloads.set(service.addJvmOverloads)
    task.sealedClassesForEnumsMatching.set(service.sealedClassesForEnumsMatching)
    task.classesForEnumsMatching.set(service.classesForEnumsMatching)
    task.generateOptionalOperationVariables.set(service.generateOptionalOperationVariables)
    task.requiresOptInAnnotation.set(service.requiresOptInAnnotation)
    task.generatePrimitiveTypes.set(service.generatePrimitiveTypes)
    val nullableFieldStyle: String? = service.nullableFieldStyle.orNull
    task.nullableFieldStyle.set(
        project.provider {
          service.nullableFieldStyle.orNull?.let {
            JavaNullable.fromName(it) ?: error("Apollo: unknown value '$nullableFieldStyle' for nullableFieldStyle")
          } ?: defaultNullableFieldStyle
        }
    )

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
    service.generateAsInternal.disallowChanges()
    service.compilerKotlinHooks.disallowChanges()

    val compilerJavaHooks = service.compilerJavaHooks.orNull ?: emptyList()
    task.compilerJavaHooks = if (compilerJavaHooks.isEmpty()) {
      ApolloCompilerJavaHooks.Identity
    } else {
      checkExternalPlugin()
      ApolloCompilerJavaHooksChain(compilerJavaHooks)
    }
    service.compilerJavaHooks.disallowChanges()
  }

  private fun registerCodeGenTask(
      project: Project,
      service: DefaultService,
  ): TaskProvider<ApolloGenerateSourcesTask> {
    return project.tasks.register(ModelNames.generateApolloSources(service), ApolloGenerateSourcesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo models for ${service.name} GraphQL queries"
      task.graphqlFiles.setFrom(service.graphqlSourceDirectorySet)
      // This has to be lazy in case the schema is not written yet during configuration
      // See the `graphql files can be generated by another task` test
      task.schemaFiles.from(project.provider { service.lazySchemaFiles(project) })
      task.targetLanguage.set(project.provider { service.targetLanguage() })
      task.warnOnDeprecatedUsages.set(service.warnOnDeprecatedUsages)
      task.failOnWarnings.set(service.failOnWarnings)
      task.scalarTypeMapping.set(service.scalarTypeMapping)
      task.scalarAdapterMapping.set(service.scalarAdapterMapping)
      task.alwaysGenerateTypesMatching.set(service.alwaysGenerateTypesMatching)
      task.projectPath.set(project.path)
      task.codegenModels.set(project.provider { service.codegenModels() })
      task.addTypename.set(service.addTypename)
      task.flattenModels.set(project.provider { service.flattenModels() })
      task.generateDataBuilders.set(service.generateDataBuilders)
      task.fieldsOnDisjointTypesMustMerge.set(service.fieldsOnDisjointTypesMustMerge)
      task.decapitalizeFields.set(service.decapitalizeFields)

      configureBaseCodegenTask(project, task, service)
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
  private fun lazySchemaFileForDownload(service: DefaultService, schemaFile: RegularFileProperty): File {
    if (schemaFile.isPresent) {
      return schemaFile.get().asFile
    }

    val candidates = service.lazySchemaFiles(project)
    check(candidates.isNotEmpty()) {
      "No schema files found. Specify introspection.schemaFile or registry.schemaFile"
    }
    check(candidates.size == 1) {
      "Multiple schema files found:\n${candidates.joinToString("\n")}\n\nSpecify introspection.schemaFile or registry.schemaFile"
    }

    return candidates.single()
  }

  private fun registerDownloadSchemaTasks(service: DefaultService) {
    val introspection = service.introspection
    var taskProvider: TaskProvider<ApolloDownloadSchemaTask>? = null
    var connection: Action<SchemaConnection>? = null

    if (introspection != null) {
      taskProvider = project.tasks.register(ModelNames.downloadApolloSchemaIntrospection(service), ApolloDownloadSchemaTask::class.java) { task ->

        task.group = TASK_GROUP
        task.outputFile.set(lazySchemaFileForDownload(service, introspection.schemaFile))
        task.endpoint.set(introspection.endpointUrl)
        task.header = introspection.headers.get().map { "${it.key}: ${it.value}" }
      }
      connection = introspection.schemaConnection
    }
    val registry = service.registry
    if (registry != null) {
      taskProvider = project.tasks.register(ModelNames.downloadApolloSchemaRegistry(service), ApolloDownloadSchemaTask::class.java) { task ->

        task.group = TASK_GROUP
        task.outputFile.set(lazySchemaFileForDownload(service, registry.schemaFile))
        task.graph.set(registry.graph)
        task.key.set(registry.key)
        task.graphVariant.set(registry.graphVariant)
      }
      connection = registry.schemaConnection
    }
    if (connection != null && taskProvider != null) {
      connection.execute(
          SchemaConnection(
              taskProvider,
              taskProvider.flatMap { downloadSchemaTask ->
                downloadSchemaTask.outputFile
              }
          )
      )
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
    hasExplicitService = true

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
    hasExplicitService = true

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
    private const val USAGE_APOLLO_IR = "apollo-ir"
    private const val USAGE_APOLLO_DOWNSTREAM_IR = "apollo-downstream-ir"
    private const val USAGE_APOLLO_SCHEMA = "apollo-schema"


    private fun getDeps(configurations: ConfigurationContainer): List<String> {
      return configurations.flatMap { configuration ->
        configuration.dependencies
            .filter {
              /**
               * When using plugins {}, the group is the plugin id, not the maven group
               */
              /**
               * the "_" check is for refreshVersions,
               * see https://github.com/jmfayard/refreshVersions/issues/507
               */
              it.group in listOf("com.apollographql.apollo3", "com.apollographql.apollo3.external")
                  && it.version != "_"
            }.mapNotNull { dependency ->
              dependency.version
            }
      }
    }

    // Don't use `graphqlSourceDirectorySet.isEmpty` here, it doesn't work for some reason
    private val SourceDirectorySet.isReallyEmpty
      get() = sourceDirectories.isEmpty

    private fun mainSourceSet(project: Project): String {
      return when (project.extensions.findByName("kotlin")) {
        is KotlinMultiplatformExtension -> "commonMain"
        else -> "main"
      }
    }

    /**
     * May return an empty set
     */
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

    internal fun Project.hasJavaPlugin() = project.extensions.findByName("java") != null
    internal fun Project.hasKotlinPlugin() = project.extensions.findByName("kotlin") != null
  }
}
