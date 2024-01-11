package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.hooks.internal.ApolloCompilerJavaHooksChain
import com.apollographql.apollo3.compiler.hooks.internal.ApolloCompilerKotlinHooksChain
import com.apollographql.apollo3.gradle.api.AndroidProject
import com.apollographql.apollo3.gradle.api.ApolloAttributes
import com.apollographql.apollo3.gradle.api.ApolloDependencies
import com.apollographql.apollo3.gradle.api.ApolloExtension
import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import com.apollographql.apollo3.gradle.api.SchemaConnection
import com.apollographql.apollo3.gradle.api.Service
import com.apollographql.apollo3.gradle.api.androidExtension
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
        upstreamProjects = service.upstreamDependencies.filterIsInstance<ProjectDependency>().map { it.name }.toSet(),
        endpointUrl = service.introspection?.endpointUrl?.orNull,
        endpointHeaders = service.introspection?.headers?.orNull,
    )
  }

  internal fun getServiceTelemetryData(): List<ApolloGradleToolingModel.TelemetryData.ServiceTelemetryData> = services.map { service ->
    DefaultServiceTelemetryData(
        codegenModels = service.codegenModels.orNull,
        warnOnDeprecatedUsages = service.warnOnDeprecatedUsages.orNull,
        failOnWarnings = service.failOnWarnings.orNull,
        operationManifestFormat = service.operationManifestFormat.orNull,
        generateKotlinModels = service.generateKotlinModels.orNull,
        languageVersion = service.languageVersion.orNull,
        useSemanticNaming = service.useSemanticNaming.orNull,
        addJvmOverloads = service.addJvmOverloads.orNull,
        generateAsInternal = service.generateAsInternal.orNull,
        generateFragmentImplementations = service.generateFragmentImplementations.orNull,
        generateQueryDocument = service.generateQueryDocument.orNull,
        generateSchema = service.generateSchema.orNull,
        generateOptionalOperationVariables = service.generateOptionalOperationVariables.orNull,
        generateDataBuilders = service.generateDataBuilders.orNull,
        generateModelBuilders = service.generateModelBuilders.orNull,
        generateMethods = service.generateMethods.orNull,
        generatePrimitiveTypes = service.generatePrimitiveTypes.orNull,
        generateInputBuilders = service.generateInputBuilders.orNull,
        nullableFieldStyle = service.nullableFieldStyle.orNull,
        decapitalizeFields = service.decapitalizeFields.orNull,
        jsExport = service.jsExport.orNull,
        addTypename = service.addTypename.orNull,
        flattenModels = service.flattenModels.orNull,
        fieldsOnDisjointTypesMustMerge = service.fieldsOnDisjointTypesMustMerge.orNull,
        generateApolloMetadata = service.generateApolloMetadata.orNull,

        // Options for which we don't mind the value but want to know they are used
        usedOptions = mutableSetOf<String>().apply {
          if (service.includes.isPresent) add("includes")
          if (service.excludes.isPresent) add("excludes")
          if (service.sourceFolder.isPresent) add("excludes")
          if (service.schemaFile.isPresent) add("schemaFile")
          if (!service.schemaFiles.isEmpty) add("schemaFiles")
          if (service.scalarAdapterMapping.isNotEmpty()) {
            add("mapScalarAdapterExpression")
          } else if (service.scalarTypeMapping.isNotEmpty()) {
            add("mapScalar")
          }
          if (service.operationIdGenerator.isPresent) add("operationIdGenerator")
          if (service.operationOutputGenerator.isPresent) add("operationOutputGenerator")
          if (service.packageNameGenerator.isPresent) add("packageNameGenerator")
          if (service.operationManifest.isPresent) add("operationManifest")
          if (service.generatedSchemaName.isPresent) add("generatedSchemaName")
          if (service.debugDir.isPresent) add("debugDir")
          if (service.sealedClassesForEnumsMatching.isPresent) add("sealedClassesForEnumsMatching")
          if (service.classesForEnumsMatching.isPresent) add("classesForEnumsMatching")
          if (service.outputDir.isPresent) add("outputDir")
          if (service.alwaysGenerateTypesMatching.isPresent) add("alwaysGenerateTypesMatching")
          if (service.compilerKotlinHooks.isPresent) add("compilerKotlinHooks")
          if (service.compilerJavaHooks.isPresent) add("compilerJavaHooks")
          if (service.introspection != null) add("introspection")
          if (service.registry != null) add("registry")
          if (service.upstreamDependencies.isNotEmpty()) add("dependsOn")
          if (service.downstreamDependencies.isNotEmpty()) add("isADependencyOf")
        },
    )
  }

  internal val serviceCount: Int
    get() = services.size

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
      }.joinToString("\n") { "    dependsOn($it)" }

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
    if (this.generateSourcesDuringGradleSync.getOrElse(false)) {
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

    val sourcesBaseTaskProvider: TaskProvider<*>

    val optionsTaskProvider = registerOptionsTask(project, service)
    if (!service.isMultiModule()) {
      sourcesBaseTaskProvider = registerSourcesTask(project, optionsTaskProvider, service)
    } else {
      val codegenSchemaConsumerConfiguration = createConfiguration(
          name = ModelNames.codegenSchemaConsumerConfiguration(service),
          isCanBeConsumed = false,
          extendsFrom = null,
          usage = USAGE_APOLLO_CODEGEN_SCHEMA,
          serviceName = service.name,
      )

      val codegenSchemaProducerConfiguration = createConfiguration(
          name = ModelNames.codegenSchemaProducerConfiguration(service),
          isCanBeConsumed = true,
          extendsFrom = codegenSchemaConsumerConfiguration,
          usage = USAGE_APOLLO_CODEGEN_SCHEMA,
          serviceName = service.name,
      )

      val upstreamIrConsumerConfiguration = createConfiguration(
          name = ModelNames.upstreamIrConsumerConfiguration(service),
          isCanBeConsumed = false,
          extendsFrom = null,
          usage = USAGE_APOLLO_UPSTREAM_IR,
          serviceName = service.name,
      )

      val upstreamIrProducerConfiguration = createConfiguration(
          name = ModelNames.upstreamIrProducerConfiguration(service),
          isCanBeConsumed = true,
          extendsFrom = upstreamIrConsumerConfiguration,
          usage = USAGE_APOLLO_UPSTREAM_IR,
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

      val codegenMetadataConsumerConfiguration = createConfiguration(
          name = ModelNames.codegenMetadataConsumerConfiguration(service),
          isCanBeConsumed = false,
          extendsFrom = null,
          usage = USAGE_APOLLO_CODEGEN_METADATA,
          serviceName = service.name,
      )

      val codegenMetadataProducerConfiguration = createConfiguration(
          name = ModelNames.codegenMetadataProducerConfiguration(service),
          isCanBeConsumed = true,
          extendsFrom = codegenMetadataConsumerConfiguration,
          usage = USAGE_APOLLO_CODEGEN_METADATA,
          serviceName = service.name,
      )

      /**
       * Tasks
       */
      val codegenSchemaTaskProvider = registerCodegenSchemaTask(
          project = project,
          service = service,
          optionsTaskProvider = optionsTaskProvider,
          schemaConsumerConfiguration = codegenSchemaConsumerConfiguration
      )

      val irOperationsTaskProvider = registerIrOperationsTask(
          project = project,
          service = service,
          schemaConsumerConfiguration = codegenSchemaConsumerConfiguration,
          schemaTaskProvider = codegenSchemaTaskProvider,
          irOptionsTaskProvider = optionsTaskProvider,
          upstreamIrFiles = upstreamIrConsumerConfiguration)

      val irSchemaTaskProvider = if (service.isSchemaModule()) {
        registerIrSchemaTask(
            project = project,
            service = service,
            downstreamIrOperations = downstreamIrConsumerConfiguration,
            codegenSchemaTaskProvider = codegenSchemaTaskProvider,
            irOperationsTaskProvider = irOperationsTaskProvider
        )
      } else {
        null
      }

      val sourcesFromIrTaskProvider = registerSourcesFromIrTask(
          project = project,
          service = service,
          schemaConsumerConfiguration = codegenSchemaConsumerConfiguration,
          generateOptionsTaskProvider = optionsTaskProvider,
          codegenSchemaTaskProvider = codegenSchemaTaskProvider,
          irSchemaTaskProvider = irSchemaTaskProvider,
          irOperationsTaskProvider = irOperationsTaskProvider,
          upstreamCodegenMetadata = codegenMetadataConsumerConfiguration
      )

      sourcesBaseTaskProvider = sourcesFromIrTaskProvider

      project.artifacts {
        it.add(codegenSchemaProducerConfiguration.name, codegenSchemaTaskProvider.flatMap { it.codegenSchemaFile }) {
          it.classifier = "apollo-codegen-schema-${service.name}"
        }
        it.add(upstreamIrProducerConfiguration.name, irOperationsTaskProvider.flatMap { it.irOperationsFile }) {
          it.classifier = "apollo-ir-${service.name}"
        }
        it.add(downstreamIrProducerConfiguration.name, irOperationsTaskProvider.flatMap { it.irOperationsFile }) {
          it.classifier = "apollo-ir-${service.name}"
        }
        it.add(codegenMetadataProducerConfiguration.name, sourcesFromIrTaskProvider.flatMap { it.metadataOutputFile }) {
          it.classifier = "apollo-codegen-metadata-${service.name}"
        }
      }

      adhocComponentWithVariants.addVariantsFromConfiguration(codegenMetadataProducerConfiguration) {}
      adhocComponentWithVariants.addVariantsFromConfiguration(upstreamIrProducerConfiguration) {}
      adhocComponentWithVariants.addVariantsFromConfiguration(codegenSchemaProducerConfiguration) {}

      service.upstreamDependencies.forEach {
        codegenSchemaConsumerConfiguration.dependencies.add(it)
        upstreamIrConsumerConfiguration.dependencies.add(it)
        codegenMetadataConsumerConfiguration.dependencies.add(it)
      }
      service.downstreamDependencies.forEach {
        downstreamIrConsumerConfiguration.dependencies.add(it)
      }
    }

    val operationOutputConnection = Service.OperationOutputConnection(
        task = sourcesBaseTaskProvider,
        operationOutputFile = sourcesBaseTaskProvider.flatMap { (it as ApolloGenerateSourcesBaseTask).operationManifestFile }
    )

    val directoryConnection = DefaultDirectoryConnection(
        project = project,
        task = sourcesBaseTaskProvider,
        outputDir = sourcesBaseTaskProvider.flatMap { (it as ApolloGenerateSourcesBaseTask).outputDir }
    )

    if (project.hasKotlinPlugin()) {
      checkKotlinPluginVersion(project)
    }

    check(service.operationOutputAction == null || service.operationManifestAction == null) {
      "Apollo: it is an error to set both operationOutputAction and operationManifestAction. Remove operationOutputAction"
    }
    if (service.operationOutputAction != null) {
      service.operationOutputAction!!.execute(operationOutputConnection)
    }
    if (service.operationManifestAction != null) {
      service.operationManifestAction!!.execute(
          Service.OperationManifestConnection(
              operationOutputConnection.task,
              operationOutputConnection.operationOutputFile
          )
      )
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

  private fun registerSourcesFromIrTask(
      project: Project,
      service: DefaultService,
      schemaConsumerConfiguration: Configuration,
      codegenSchemaTaskProvider: TaskProvider<ApolloGenerateCodegenSchemaTask>,
      generateOptionsTaskProvider: TaskProvider<ApolloGenerateOptionsTask>,
      irSchemaTaskProvider: TaskProvider<ApolloGenerateIrSchemaTask>?,
      irOperationsTaskProvider: TaskProvider<ApolloGenerateIrOperationsTask>,
      upstreamCodegenMetadata: Configuration,
  ): TaskProvider<ApolloGenerateSourcesFromIrTask> {
    return project.tasks.register(ModelNames.generateApolloSourcesFromIr(service), ApolloGenerateSourcesFromIrTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo models for service '${service.name}'"

      configureBaseCodegenTask(project, task, generateOptionsTaskProvider, service)

      task.codegenSchemas.from(schemaConsumerConfiguration)
      task.codegenSchemas.from(codegenSchemaTaskProvider.flatMap { it.codegenSchemaFile })
      task.irOperations.set(irOperationsTaskProvider.flatMap { it.irOperationsFile })
      task.upstreamMetadata.from(upstreamCodegenMetadata)
      if (irSchemaTaskProvider != null) {
        task.irSchema.set(irSchemaTaskProvider.flatMap { it.irSchemaFile })
      }
      task.metadataOutputFile.set(BuildDirLayout.metadata(project, service))
    }
  }

  private fun registerIrSchemaTask(
      project: Project,
      service: DefaultService,
      downstreamIrOperations: Configuration,
      codegenSchemaTaskProvider: TaskProvider<ApolloGenerateCodegenSchemaTask>,
      irOperationsTaskProvider: TaskProvider<ApolloGenerateIrOperationsTask>,
  ): TaskProvider<ApolloGenerateIrSchemaTask> {
    return project.tasks.register(ModelNames.generateApolloIrSchema(service), ApolloGenerateIrSchemaTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo Used Coordinates for service '${service.name}'"

      task.codegenSchemaFile.set(codegenSchemaTaskProvider.flatMap { it.codegenSchemaFile })
      task.irSchemaFile.set(BuildDirLayout.usedCoordinates(project, service))
      task.downStreamIrOperations.from(downstreamIrOperations)
      task.irOperations.set(irOperationsTaskProvider.flatMap { it.irOperationsFile })
    }
  }

  private fun registerOptionsTask(
      project: Project,
      service: DefaultService,
  ): TaskProvider<ApolloGenerateOptionsTask> {
    return project.tasks.register(ModelNames.generateApolloOptions(service), ApolloGenerateOptionsTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo options for service '${service.name}'"

      /**
       * CodegenSchemaOptions
       */
      task.targetLanguage.set(service.targetLanguage())
      task.scalarTypeMapping.set(service.scalarTypeMapping)
      task.scalarAdapterMapping.set(service.scalarAdapterMapping)
      task.codegenModels.set(service.codegenModels)
      task.generateDataBuilders.set(service.generateDataBuilders)

      task.codegenSchemaOptionsFile.set(BuildDirLayout.codegenSchemaOptions(project, service))

      /**
       * IrOptions
       */
      task.addTypename.set(service.addTypename)
      task.fieldsOnDisjointTypesMustMerge.set(service.fieldsOnDisjointTypesMustMerge)
      task.decapitalizeFields.set(service.decapitalizeFields)
      task.flattenModels.set(service.flattenModels)
      task.warnOnDeprecatedUsages.set(service.warnOnDeprecatedUsages)
      task.failOnWarnings.set(service.failOnWarnings)
      task.generateOptionalOperationVariables.set(service.generateOptionalOperationVariables)
      task.alwaysGenerateTypesMatching.set(service.alwaysGenerateTypesMatching())

      task.irOptionsFile.set(BuildDirLayout.irOptions(project, service))

      /**
       * CommonCodegenOptions
       */
      task.packageName.set(service.packageName)
      task.packageNamesFromFilePaths.set(service.packageNamesFromFilePaths)
      task.useSemanticNaming.set(service.useSemanticNaming)
      task.generateFragmentImplementations.set(service.generateFragmentImplementations)
      task.generateMethods.set(service.generateMethods.map { list ->
        list.map {
          GeneratedMethod.fromName(it) ?: error("Apollo: unknown method type: $it for generateMethods")
        }
      })
      task.generateQueryDocument.set(service.generateQueryDocument)
      task.generateSchema.set(service.generateSchema)
      task.generatedSchemaName.set(service.generatedSchemaName)
      task.operationManifestFormat.set(service.operationManifestFormat())

      /**
       * JavaCodegenOptions
       */
      task.generateModelBuilders.set(service.generateModelBuilders)
      task.classesForEnumsMatching.set(service.classesForEnumsMatching)
      task.generatePrimitiveTypes.set(service.generatePrimitiveTypes)
      task.nullableFieldStyle.set(service.nullableFieldStyle.orNull?.let { JavaNullable.fromName(it) })

      /**
       * KotlinCodegenOptions
       */
      task.sealedClassesForEnumsMatching.set(service.sealedClassesForEnumsMatching)
      task.generateAsInternal.set(service.generateAsInternal)
      task.generateFilterNotNull.set(service.generateFilterNotNull())
      task.generateInputBuilders.set(service.generateInputBuilders)
      task.addJvmOverloads.set(service.addJvmOverloads)
      task.requiresOptInAnnotation.set(service.requiresOptInAnnotation)
      task.jsExport.set(service.jsExport)

      task.codegenOptions.set(BuildDirLayout.codegenOptions(project, service))
    }
  }

  private fun registerIrOperationsTask(
      project: Project,
      service: DefaultService,
      schemaConsumerConfiguration: Configuration,
      schemaTaskProvider: TaskProvider<ApolloGenerateCodegenSchemaTask>,
      irOptionsTaskProvider: TaskProvider<ApolloGenerateOptionsTask>,
      upstreamIrFiles: Configuration,
  ): TaskProvider<ApolloGenerateIrOperationsTask> {
    return project.tasks.register(ModelNames.generateApolloIrOperations(service), ApolloGenerateIrOperationsTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo IR operations for service '${service.name}'"

      task.codegenSchemaFiles.from(schemaConsumerConfiguration)
      task.codegenSchemaFiles.from(schemaTaskProvider.flatMap { it.codegenSchemaFile })
      task.graphqlFiles.setFrom(service.graphqlSourceDirectorySet)
      task.upstreamIrFiles.from(upstreamIrFiles)
      task.irOptionsFile.set(irOptionsTaskProvider.flatMap { it.irOptionsFile })

      task.irOperationsFile.set(BuildDirLayout.ir(project, service))
    }
  }

  private fun registerCodegenSchemaTask(
      project: Project,
      service: DefaultService,
      optionsTaskProvider: TaskProvider<ApolloGenerateOptionsTask>,
      schemaConsumerConfiguration: Configuration,
  ): TaskProvider<ApolloGenerateCodegenSchemaTask> {
    return project.tasks.register(ModelNames.generateApolloCodegenSchema(service), ApolloGenerateCodegenSchemaTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo schema for service '${service.name}'"

      // This has to be lazy in case the schema is not written yet during configuration
      // See the `graphql files can be generated by another task` test
      task.schemaFiles.from(project.provider { service.lazySchemaFiles(project) })
      task.upstreamSchemaFiles.from(schemaConsumerConfiguration)
      task.codegenSchemaOptionsFile.set(optionsTaskProvider.flatMap { it.codegenSchemaOptionsFile })
      if (service.packageNamesFromFilePaths) {
        task.packageNameRoots = service.graphqlSourceDirectorySet.srcDirs.map { it.absolutePath }.toSet()
      }
      task.packageNameGenerator = service.packageNameGenerator.orNull
      task.userGenerateKotlinModels.set(service.generateKotlinModels)
      task.userCodegenModels.set(service.codegenModels)
      task.codegenSchemaFile.set(BuildDirLayout.schema(project, service))
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

        task.operationManifestFormat.set(service.operationManifestFormat())
        task.listId.set(registerOperationsConfig.listId)
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
      task: ApolloGenerateSourcesBaseTask,
      generateOptionsTask: TaskProvider<ApolloGenerateOptionsTask>,
      service: DefaultService,
  ) {
    task.graphqlFiles.from(service.graphqlSourceDirectorySet)
    task.codegenOptionsFile.set(generateOptionsTask.flatMap { it.codegenOptions })

    task.setPackageNameProperties(service)
    service.packageNameGenerator.disallowChanges()

    task.operationOutputGenerator = service.operationOutputGenerator.orElse(service.operationIdGenerator.map { OperationOutputGenerator.Default(it) }).orNull
    service.operationOutputGenerator.disallowChanges()

    task.compilerKotlinHooks = service.compilerKotlinHooks.orNull?.let { ApolloCompilerKotlinHooksChain(it) }
    service.compilerKotlinHooks.disallowChanges()

    task.compilerJavaHooks = service.compilerJavaHooks.orNull?.let { ApolloCompilerJavaHooksChain(it) }
    service.compilerJavaHooks.disallowChanges()

    task.outputDir.set(service.outputDir.orElse(BuildDirLayout.outputDir(project, service)))
    task.operationManifestFile.set(service.operationManifestFile())
  }

  private fun registerSourcesTask(
      project: Project,
      optionsTaskProvider: TaskProvider<ApolloGenerateOptionsTask>,
      service: DefaultService,
  ): TaskProvider<ApolloGenerateSourcesTask> {
    return project.tasks.register(ModelNames.generateApolloSources(service), ApolloGenerateSourcesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Apollo models for service '${service.name}'"

      configureBaseCodegenTask(project, task, optionsTaskProvider, service)

      // This has to be lazy in case the schema is not written yet during configuration
      // See the `graphql files can be generated by another task` test
      task.schemaFiles.from(project.provider { service.lazySchemaFiles(project) })
      task.codegenSchemaOptionsFile.set(optionsTaskProvider.map { it.codegenSchemaOptionsFile.get() })
      task.irOptionsFile.set(optionsTaskProvider.map { it.irOptionsFile.get() })
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
    const val MIN_GRADLE_VERSION = "6.8"

    private const val USAGE_APOLLO_CODEGEN_METADATA = "apollo-codegen-metadata"
    private const val USAGE_APOLLO_UPSTREAM_IR = "apollo-upstream-ir"
    private const val USAGE_APOLLO_DOWNSTREAM_IR = "apollo-downstream-ir"
    private const val USAGE_APOLLO_CODEGEN_SCHEMA = "apollo-codegen-schema"

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

  override fun apolloKspProcessor(schema: File, service: String, packageName: String): Any {
    check(project.pluginManager.hasPlugin("com.google.devtools.ksp")) {
      "Calling apolloKspProcessor only makes sense if the 'com.google.devtools.ksp' plugin is applied"
    }

    val producer = project.configurations.create("apollo${service.capitalizeFirstLetter()}KspProcessorProducer") {
      it.isCanBeResolved = false
      it.isCanBeConsumed = true
    }

    producer.dependencies.add(project.dependencies.create("com.apollographql.apollo3:apollo-ksp-incubating"))
    val taskProvider = project.tasks.register("generate${service.capitalizeFirstLetter()}ApolloKspProcessor", ApolloGenerateKspProcessorTask::class.java) {
      it.schema.set(schema)
      it.serviceName.set(service)
      it.packageName.set(packageName)
      it.outputJar.set(BuildDirLayout.kspProcessorJar(project, service))
    }

    project.artifacts.add(producer.name, taskProvider.flatMap { it.outputJar })

    return project.dependencies.project(mapOf("path" to project.path, "configuration" to producer.name))
  }

  override val deps: ApolloDependencies = ApolloDependencies(project.dependencies)
}
