package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloDependencies
import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.ApolloGradleToolingModel
import com.apollographql.apollo.gradle.api.SchemaConnection
import com.apollographql.apollo.gradle.api.Service
import com.apollographql.apollo.gradle.internal.BuildDirLayout.dataBuildersOutputDir
import com.apollographql.apollo.gradle.internal.BuildDirLayout.outputDir
import com.apollographql.apollo.gradle.task.ApolloDownloadSchemaTask
import com.apollographql.apollo.gradle.task.ApolloGenerateDataBuildersSourcesTask
import com.apollographql.apollo.gradle.task.ApolloGenerateSourcesFromIrTask
import com.apollographql.apollo.gradle.task.ApolloGenerateSourcesTask
import com.apollographql.apollo.gradle.task.registerApolloComputeUsedCoordinatesTask
import com.apollographql.apollo.gradle.task.registerApolloDownloadSchemaTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateCodegenSchemaTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateDataBuildersSourcesTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateIrOperationsTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateOptionsTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateSourcesFromIrTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateSourcesTask
import com.apollographql.apollo.gradle.task.registerApolloRegisterOperationsTask
import gratatouille.wiring.capitalizeFirstLetter
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

abstract class DefaultApolloExtension(
    private val project: Project,
) : ApolloExtension {

  private var codegenOnGradleSyncConfigured: Boolean = false
  private val services = mutableListOf<DefaultService>()
  private val checkVersionsTask: TaskProvider<Task>
  private val generateApolloSources: TaskProvider<Task>
  private var hasExplicitService = false
  private val adhocComponentWithVariants: AdhocComponentWithVariants by lazy {
    softwareComponentFactory.adhoc("apollo").also {
      project.components.add(it)
    }
  }

  internal fun getServiceInfos(project: Project): List<ApolloGradleToolingModel.ServiceInfo> = services.map { service ->
    DefaultServiceInfo(
        name = service.name,
        schemaFiles = service.schemaFilesSnapshot(project),
        graphqlSrcDirs = service.graphqlSourceDirectorySet.srcDirs,
        upstreamProjects = service.upstreamDependencies.filterIsInstance<ProjectDependency>().map { it.name }.toSet(),
        upstreamProjectPaths = service.upstreamDependencies.filterIsInstance<ProjectDependency>().map { it.getPathCompat() }.toSet(),
        endpointUrl = service.introspection?.endpointUrl?.orNull,
        endpointHeaders = service.introspection?.headers?.orNull,
        useSemanticNaming = service.useSemanticNaming.getOrElse(true),
    )
  }

  internal fun registerDownstreamProject(serviceName: String, projectPath: String) {
    project.configurations.configureEach {
      if (it.name == ModelNames.scopeConfiguration(serviceName, ApolloDirection.Downstream)) {
        it.dependencies.add(project.dependencies.project(mapOf("path" to projectPath)))
      }
    }
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
          if (service.schemaFile.isPresent) add("schemaFile")
          if (!service.schemaFiles.isEmpty) add("schemaFiles")
          if (service.scalarAdapterMapping.isNotEmpty()) {
            add("mapScalarAdapterExpression")
          } else if (service.scalarTypeMapping.isNotEmpty()) {
            add("mapScalar")
          }
          @Suppress("DEPRECATION_ERROR")
          if (service.operationManifest.isPresent) add("operationManifest")
          if (service.generatedSchemaName.isPresent) add("generatedSchemaName")
          if (service.debugDir.isPresent) add("debugDir")
          if (service.sealedClassesForEnumsMatching.isPresent) add("sealedClassesForEnumsMatching")
          if (service.classesForEnumsMatching.isPresent) add("classesForEnumsMatching")
          @Suppress("DEPRECATION_ERROR")
          if (service.outputDir.isPresent) add("outputDir")
          if (service.alwaysGenerateTypesMatching.isPresent) add("alwaysGenerateTypesMatching")
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
      "apollo-kotlin requires Gradle version $MIN_GRADLE_VERSION or greater"
    }

    checkVersionsTask = registerCheckVersionsTask()

    /**
     * An aggregate task to easily generate all models
     */
    generateApolloSources = project.tasks.register(ModelNames.generateApolloSources()) {
      it.group = TASK_GROUP
      it.description = "Generate Apollo models for all services"
    }

    /**
     * A simple task to be used from the command line to ease the schema download (deprecated)
     */
    project.tasks.register(ModelNames.downloadApolloSchema()) { task ->
      task.group = TASK_GROUP
      task.doLast {
        error("Apollo: using './gradlew downloadApolloSchema' is deprecated. Please use the Apollo Kotlin cli for one-time downloads or the introspection {} block for Gradle downloads. See https://go.apollo.dev/ak-download-schema.")
      }
    }

    /**
     * A simple task to be used from the command line to ease schema conversion
     */
    project.tasks.register(ModelNames.convertApolloSchema()) { task ->
      task.group = TASK_GROUP
      task.doLast {
        error("Apollo: using './gradlew convertApolloSchema' is deprecated. Please use the Apollo Kotlin cli for converseions. See https://go.apollo.dev/ak-cli.")
      }
    }

    /**
     * A simple task to be used from the command line to ease the schema upload
     */
    project.tasks.register(ModelNames.pushApolloSchema()) { task ->
      task.group = TASK_GROUP
      task.doLast {
        error("Apollo: using './gradlew pushApolloSchema' is deprecated. Please use rover to push schemas. See https://go.apollo.dev/rover.")
      }
    }

    project.afterEvaluate {
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
    when (linkSqlite.orNull) {
      false -> return // explicit opt-out
      true -> {
        // explicit opt-in
        linkSqlite(project)
      }

      null -> { // default: automatic detection
        project.configurations.configureEach {
          it.dependencies.configureEach {
            /*
             * Try to detect if a native version of apollo-normalized-cache-sqlite is in the classpath
             * This is a heuristic and will not work in 100% of the cases.
             *
             * Note: we only check external dependencies as reading the group of project dependencies
             * is not compatible with isolated projects
             */
            if (it is ExternalModuleDependency
                && it.group?.contains("apollo") == true
                && it.name.contains("normalized-cache-sqlite")
                && !it.name.contains("jvm")
                && !it.name.contains("android")
            ) {
              linkSqlite(project)
            }
          }
        }
      }
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
    sanityChecks(service)

    maybeConfigureCodegenOnGradleSync()
  }

  private fun sanityChecks(service: DefaultService) {
    @Suppress("DEPRECATION_ERROR")
    check(!service.sourceFolder.isPresent) {
      error("Apollo: using 'sourceFolder' is deprecated, replace with 'srcDir(\"src/${project.mainSourceSet()}/graphql/${service.sourceFolder.get()}\")'")
    }
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

  /**
   * Registers the `checkVersions` task.
   *
   * `checkVersions` ensures that all declared versions in a build are the same (plugins, direct dependencies but not transitive dependencies).
   * The main goal is to make sure that the generated code matches the `apollo-api` version as we historically do not provide compatibility guarantees.
   *
   * This code has some shortcomings:
   * 1. it is too restrictive. Most of the time, codegen x is compatible with runtime y as long as y >= x and the same major version.
   * 2. it doesn't work with transitive dependencies. This is fine because Gradle by default uses the greatest version and because of 1. it works most of the time.
   * 3. it's a global check and there _could_ be scenarios where this is not desirable.
   *
   * All of this makes this check ill-defined, but it hasn't been too much of an issue so far, and it's a net gain to catch the plugin/runtime discrepancies that have happened in the past.
   *
   * If you're reading this because there has been an issue, there are several mitigations:
   *
   * ## Disabling the task
   *
   * This is the most immediate and easy solution:
   *
   * ```kotlin
   * tasks.named("checkApolloVersions").configure {enabled = false}
   * ```
   * ## runtime check
   *
   * More involved but more correct, check at runtime that the versions match. Requires adding the codegen version in generated sources:
   *
   * - a new field in [com.apollographql.apollo.api.Operation].
   * - or binding an [com.apollographql.apollo.ApolloClient] to a given schema (could be useful for other purposes as well such as schema testing).
   *
   * ## automatically add the `apollo-api` dependency
   *
   * That would have the effect of making sure a compatible `apollo-api` is in the classpath. But won't help if `apollo-runtime` is wrong.
   *
   * All in all, the current solution works but if it becomes an issue, do not hesitate to revisit it.
   */
  // Gradle will consider the task never UP-TO-DATE if we pass a lambda to doLast()
  @Suppress("ObjectLiteralToLambda")
  private fun registerCheckVersionsTask(): TaskProvider<Task> {
    return project.tasks.register(ModelNames.checkApolloVersions()) {
      val outputFile = BuildDirLayout.versionCheck(project)

      it.inputs.property("allVersions", Callable {
        val allDeps = (
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

  class Configurations(
      val consumable: Configuration,
      val resolvable: Configuration,
  )

  private fun createConfigurations(
      serviceName: String,
      apolloUsage: ApolloUsage,
      direction: ApolloDirection,
      extendsFrom: Configuration,
  ): Configurations {
    val consumable =
      project.configurations.create(ModelNames.configuration(serviceName, direction, apolloUsage, ConfigurationKind.Consumable)) {
        it.isCanBeConsumed = true
        it.isCanBeResolved = false

        it.extendsFrom(extendsFrom)
        it.attributes(serviceName, apolloUsage, direction)
      }
    val resolvable =
      project.configurations.create(ModelNames.configuration(serviceName, direction, apolloUsage, ConfigurationKind.Resolvable)) {
        it.isCanBeConsumed = false
        it.isCanBeResolved = true

        it.extendsFrom(extendsFrom)
        it.attributes(serviceName, apolloUsage, direction)
      }

    return Configurations(
        consumable = consumable,
        resolvable = resolvable
    )
  }

  private fun <T> HasConfigurableAttributes<T>.attributes(serviceName: String, usage: ApolloUsage, direction: ApolloDirection) {
    attributes {
      it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, usage.name))
      it.attribute(APOLLO_SERVICE_ATTRIBUTE, serviceName)
      it.attribute(APOLLO_DIRECTION_ATTRIBUTE, direction.name)
    }
  }

  private fun registerService(service: DefaultService) {
    check(services.find { it.name == service.name } == null) {
      "There is already a service named ${service.name}, please use another name"
    }
    services.add(service)

    if (service.graphqlSourceDirectorySet.isReallyEmpty) {
      val dir = File(project.projectDir, "src/${project.mainSourceSet()}/graphql/")
      service.graphqlSourceDirectorySet.srcDir(dir)
    }
    service.graphqlSourceDirectorySet.include(service.includes.getOrElse(listOf("**/*.graphql", "**/*.gql")))
    service.graphqlSourceDirectorySet.exclude(service.excludes.getOrElse(emptyList()))

    val sourcesBaseTaskProvider: TaskProvider<*>
    val dataBuildersSourcesBaseTaskProvider: TaskProvider<*>?

    val upstreamScope = project.configurations.create(ModelNames.scopeConfiguration(service.name, ApolloDirection.Upstream)) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = false
    }
    val downstreamScope = project.configurations.create(ModelNames.scopeConfiguration(service.name, ApolloDirection.Downstream)) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = false
    }

    val otherOptions = createConfigurations(
        serviceName = service.name,
        apolloUsage = ApolloUsage.OtherOptions,
        direction = ApolloDirection.Upstream,
        extendsFrom = upstreamScope
    )

    val compilerConfiguration = project.configurations.create(ModelNames.compilerConfiguration(service)) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true
    }

    service.pluginDependency?.let {
      compilerConfiguration.dependencies.add(it)
    }

    val warnIfNoPluginFound = service.pluginDependency != null
    val pluginArguments = (service.compilerPlugin as DefaultCompilerPlugin?)?.arguments.orEmpty()

    if (service.languageVersion.orNull == "1.5") {
      project.logger.lifecycle("Apollo: languageVersion 1.5 is deprecated, please use 1.9 or leave empty")
    }
    val optionsTaskProvider = project.registerApolloGenerateOptionsTask(
        taskName = ModelNames.generateApolloOptions(service),
        taskGroup = TASK_GROUP,
        taskDescription = "Generate Apollo options for service '${service.name}'",
        /**
         * CodegenSchemaOptions
         */
        scalarTypeMapping = project.provider { service.scalarTypeMapping },
        scalarAdapterMapping = project.provider { service.scalarAdapterMapping },
        generateDataBuilders = service.generateDataBuilders,
        /**
         * IrOptions
         */
        codegenModels = service.codegenModels,
        addTypename = service.addTypename,
        fieldsOnDisjointTypesMustMerge = service.fieldsOnDisjointTypesMustMerge,
        decapitalizeFields = service.decapitalizeFields,
        flattenModels = service.flattenModels,
        warnOnDeprecatedUsages = service.warnOnDeprecatedUsages,
        failOnWarnings = service.failOnWarnings,
        generateOptionalOperationVariables = service.generateOptionalOperationVariables,
        alwaysGenerateTypesMatching = service.alwaysGenerateTypesMatching,

        /**
         * CommonCodegenOptions
         */
        generateKotlinModels = service.generateKotlinModels,
        languageVersion = service.languageVersion,
        packageName = service.packageName,
        rootPackageName = project.provider { service.rootPackageName },
        useSemanticNaming = service.useSemanticNaming,
        generateFragmentImplementations = service.generateFragmentImplementations,
        generateMethods = service.generateMethods,
        generateQueryDocument = service.generateQueryDocument,
        generateSchema = service.generateSchema,
        generatedSchemaName = service.generatedSchemaName,
        operationManifestFormat = service.operationManifestFormat,

        /**
         * JavaCodegenOptions
         */
        generateModelBuilders = service.generateModelBuilders,
        classesForEnumsMatching = service.classesForEnumsMatching,
        generatePrimitiveTypes = service.generatePrimitiveTypes,
        nullableFieldStyle = service.nullableFieldStyle,

        /**
         * KotlinCodegenOptions
         */
        sealedClassesForEnumsMatching = service.sealedClassesForEnumsMatching,
        generateAsInternal = service.generateAsInternal,
        generateInputBuilders = service.generateInputBuilders,
        addJvmOverloads = service.addJvmOverloads,
        requiresOptInAnnotation = service.requiresOptInAnnotation,
        jsExport = service.jsExport,

        /**
         * Gradle model
         */
        upstreamOtherOptions = otherOptions.resolvable,
        javaPluginApplied = project.provider { project.hasJavaPlugin() },
        kgpVersion = project.provider { project.apolloGetKotlinPluginVersion() },
        kmp = project.provider { project.isKotlinMultiplatform },
        // If there is no downstream dependency, generate everything because we don't know what types are going to be used downstream
        generateAllTypes = project.provider { service.isSchemaModule() && service.isMultiModule() && service.downstreamDependencies.isEmpty() },
    )

    if (!service.isMultiModule()) {
      sourcesBaseTaskProvider = project.registerApolloGenerateSourcesTask(
          taskName = ModelNames.generateApolloSources(service),
          taskGroup = TASK_GROUP,
          taskDescription = "Generate Apollo models for service '${service.name}'",
          extraClasspath = compilerConfiguration,
          arguments = project.provider { pluginArguments },
          warnIfNotFound = project.provider { warnIfNoPluginFound },
          schemas = service.schemaFiles(project),
          fallbackSchemas = service.fallbackSchemaFiles(project),
          irOptions = optionsTaskProvider.flatMap { it.irOptionsFile },
          codegenSchemaOptions = optionsTaskProvider.flatMap { it.codegenSchemaOptionsFile },
          codegenOptions = optionsTaskProvider.flatMap { it.codegenOptions },
          executableDocuments = service.graphqlSourceDirectorySet,
          outputDirectory = outputDir(project, service),
          operationManifest = BuildDirLayout.operationManifest(project, service),
          dataBuildersOutputDirectory = dataBuildersOutputDir(project, service)
      )
      dataBuildersSourcesBaseTaskProvider = if (service.generateDataBuilders.orElse(false).get()) {
        // Only register the wiring if we actually generate data builders
        sourcesBaseTaskProvider
      } else {
        null
      }
    } else {
      val codegenSchema = createConfigurations(
          serviceName = service.name,
          apolloUsage = ApolloUsage.CodegenSchema,
          direction = ApolloDirection.Upstream,
          extendsFrom = upstreamScope
      )

      val upstreamIr = createConfigurations(
          serviceName = service.name,
          apolloUsage = ApolloUsage.Ir,
          direction = ApolloDirection.Upstream,
          extendsFrom = upstreamScope
      )

      val downstreamIr = createConfigurations(
          serviceName = service.name,
          apolloUsage = ApolloUsage.Ir,
          direction = ApolloDirection.Downstream,
          extendsFrom = downstreamScope
      )

      val codegenMetadata = createConfigurations(
          serviceName = service.name,
          apolloUsage = ApolloUsage.CodegenMetadata,
          direction = ApolloDirection.Upstream,
          extendsFrom = upstreamScope
      )

      /**
       * Tasks
       */
      val codegenSchemaTaskProvider = if (service.isSchemaModule()) {
        project.registerApolloGenerateCodegenSchemaTask(
            taskName = ModelNames.generateApolloCodegenSchema(service),
            taskGroup = TASK_GROUP,
            taskDescription = "Generate Apollo schema for service '${service.name}'",
            schemaFiles = service.schemaFiles(project),
            fallbackSchemaFiles = service.fallbackSchemaFiles(project),
            upstreamSchemaFiles = codegenSchema.resolvable,
            codegenSchemaOptionsFile = optionsTaskProvider.flatMap { it.codegenSchemaOptionsFile },
            arguments = project.provider { pluginArguments },
            extraClasspath = compilerConfiguration,
            warnIfNotFound = project.provider { warnIfNoPluginFound },
        )
      } else {
        check(service.scalarTypeMapping.isEmpty()) {
          "Apollo: the custom scalar configuration is not used in non-schema modules. Add custom scalars to your schema module."
        }
        check(!service.generateDataBuilders.isPresent) {
          "Apollo: generateDataBuilders is not used in non-schema modules. Add generateDataBuilders to your schema module."
        }

        null
      }

      val upstreamAndSelfCodegenSchemas = project.files().also {
        it.from(codegenSchema.resolvable)
        if (codegenSchemaTaskProvider != null) {
          it.from(codegenSchemaTaskProvider.flatMap { it.codegenSchemaFile })
        }
      }
      val irOperationsTaskProvider = project.registerApolloGenerateIrOperationsTask(
          taskName = ModelNames.generateApolloIrOperations(service),
          taskGroup = TASK_GROUP,
          taskDescription = "Generate Apollo IR operations for service '${service.name}'",
          extraClasspath = compilerConfiguration,
          arguments = project.provider { pluginArguments },
          warnIfNotFound = project.provider { warnIfNoPluginFound },
          upstreamIrFiles = upstreamIr.resolvable,
          codegenSchemas = upstreamAndSelfCodegenSchemas,
          graphqlFiles = service.graphqlSourceDirectorySet,
          irOptionsFile = optionsTaskProvider.flatMap { it.irOptionsFile },
      )

      val computeUsedCoordinatesTask = project.registerApolloComputeUsedCoordinatesTask(
          taskName = ModelNames.computeUsedCoordinates(service),
          taskGroup = TASK_GROUP,
          irOperations = downstreamIr.resolvable,
      )
      val sourcesFromIrTaskProvider = project.registerApolloGenerateSourcesFromIrTask(
          taskName = ModelNames.generateApolloSources(service),
          taskGroup = TASK_GROUP,
          taskDescription = "Generate Apollo models for service '${service.name}'",
          extraClasspath = compilerConfiguration,
          arguments = project.provider { pluginArguments },
          warnIfNotFound = project.provider { warnIfNoPluginFound },
          codegenSchemas = upstreamAndSelfCodegenSchemas,
          usedCoordinates = computeUsedCoordinatesTask.flatMap { it.outputFile },
          irOperations = irOperationsTaskProvider.flatMap { it.irOperationsFile },
          upstreamMetadata = codegenMetadata.resolvable,
          codegenOptions = optionsTaskProvider.flatMap { it.codegenOptions },
          outputDirectory = outputDir(project, service),
          operationManifest = BuildDirLayout.operationManifest(project, service)
      )

      sourcesBaseTaskProvider = sourcesFromIrTaskProvider

      val upstreamAndSelfCodegenMetadata = project.files().apply {
        from(codegenMetadata.resolvable)
        from(sourcesFromIrTaskProvider.flatMap { it.metadataOutput })
      }

      dataBuildersSourcesBaseTaskProvider = if (service.generateDataBuilders.orElse(false).get()) {
        project.registerApolloGenerateDataBuildersSourcesTask(
            taskName = ModelNames.generateDataBuildersApolloSources(service),
            taskGroup = TASK_GROUP,
            taskDescription = "Generate Apollo data builders for service '${service.name}'",
            extraClasspath = compilerConfiguration,
            arguments = project.provider { pluginArguments },
            warnIfNotFound = project.provider { warnIfNoPluginFound },
            codegenSchemas = upstreamAndSelfCodegenSchemas,
            downstreamUsedCoordinates = computeUsedCoordinatesTask.flatMap { it.outputFile },
            upstreamMetadata = upstreamAndSelfCodegenMetadata,
            codegenOptions = optionsTaskProvider.flatMap { it.codegenOptions },
            outputDirectory = dataBuildersOutputDir(project, service)
        )
      } else {
        null
      }

      project.artifacts {
        if (codegenSchemaTaskProvider != null) {
          it.add(codegenSchema.consumable.name, codegenSchemaTaskProvider.flatMap { it.codegenSchemaFile }) {
            it.classifier = "codegen-schema-${service.name}"
          }
          it.add(otherOptions.consumable.name, optionsTaskProvider.flatMap { it.otherOptions }) {
            it.classifier = "other-options-${service.name}"
          }
        }
        it.add(upstreamIr.consumable.name, irOperationsTaskProvider.flatMap { it.irOperationsFile }) {
          it.classifier = "ir-${service.name}"
        }
        it.add(downstreamIr.consumable.name, irOperationsTaskProvider.flatMap { it.irOperationsFile }) {
          it.classifier = "ir-${service.name}"
        }
        it.add(codegenMetadata.consumable.name, sourcesFromIrTaskProvider.flatMap { it.metadataOutput }) {
          it.classifier = "codegen-metadata-${service.name}"
        }
      }

      /*
       * Note: no component is created to publish the downstreamIr. This is because that would
       * require publishing in 2 phases:
       * - publish the downstream Ir
       * - schema module uses that for used coordinates and publishes the codegen metadata
       * - downstream module publishes jar
       * In such scenarios, the user must set `alwaysGenerateTypesMatching.set(listOf(".*"))` on the schema module.
       */
      val outgoingVariantsConnection = object : Service.OutgoingVariantsConnection {
        override fun addToSoftwareComponent(name: String) {
          addToSoftwareComponent(project.components.getByName(name))
        }

        override fun addToSoftwareComponent(softwareComponent: SoftwareComponent) {
          check(softwareComponent is AdhocComponentWithVariants) {
            "Software component '$softwareComponent' is not an instance of AdhocComponentWithVariants"
          }
          outgoingVariants.forEach {
            softwareComponent.addVariantsFromConfiguration(it) { }
          }
        }

        override val outgoingVariants: List<Configuration>
          get() = listOf(codegenMetadata.consumable, upstreamIr.consumable, codegenSchema.consumable, otherOptions.consumable)
      }
      if (service.outgoingVariantsConnection != null) {
        service.outgoingVariantsConnection!!.execute(outgoingVariantsConnection)
      } else {
        outgoingVariantsConnection.addToSoftwareComponent(adhocComponentWithVariants)
      }

      service.upstreamDependencies.forEach {
        upstreamScope.dependencies.add(it)
      }

      service.downstreamDependencies.forEach {
        downstreamScope.dependencies.add(it)
      }
    }

    val operationOutputConnection = Service.OperationOutputConnection(
        task = sourcesBaseTaskProvider,
        operationOutputFile = sourcesBaseTaskProvider.flatMap { it.operationManifest() }
    )

    @Suppress("DEPRECATION_ERROR")
    check(!service.outputDir.isPresent) {
      "Apollo: changing the location of the generated sources is not possible anymore. Use `outputDirConnection` to consume them and potentially copy them to another filesystem location."
    }
    val directoryConnection = DefaultDirectoryConnection(
        project = project,
        task = sourcesBaseTaskProvider,
        outputDir = sourcesBaseTaskProvider.flatMap { it.outputDirectory() },
        hardCodedOutputDir = outputDir(project, service)
    )

    if (project.hasKotlinPlugin()) {
      checkKotlinPluginVersion(project)
    }


    @Suppress("DEPRECATION_ERROR")
    check(!service.operationManifest.isPresent) {
      "Apollo: changing the location of the generated manifest is not possible anymore. Use `operationManifestConnection` to consume it and potentially copy it to another filesystem location."
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
    val registerOperationsConfig = service.registerOperationsConfig
    if (registerOperationsConfig != null) {
      project.registerApolloRegisterOperationsTask(
          taskName = ModelNames.registerApolloOperations(service),
          taskGroup = TASK_GROUP,
          operationOutput = operationOutputConnection.operationOutputFile,
          operationManifestFormat = service.operationManifestFormat,
          listId = registerOperationsConfig.listId,
          graphVariant = registerOperationsConfig.graphVariant,
          key = registerOperationsConfig.key,
          graph = registerOperationsConfig.graph,
      )
    }

    if (service.outputDirAction == null) {
      service.outputDirAction = defaultOutputDirAction
    }
    service.outputDirAction!!.execute(directoryConnection)

    directoryConnection.task.configure {
      it.dependsOn(checkVersionsTask)
    }

    if (dataBuildersSourcesBaseTaskProvider != null) {
      val dataBuildersDirectoryConnection = DefaultDirectoryConnection(
          project = project,
          task = dataBuildersSourcesBaseTaskProvider,
          outputDir = dataBuildersSourcesBaseTaskProvider.flatMap {
            it.dataBuildersOutputDirectory()
          },
          hardCodedOutputDir = dataBuildersOutputDir(project, service)
      )

      if (service.dataBuildersOutputDirAction == null) {
        service.dataBuildersOutputDirAction = defaultDataBuildersOutputDirAction
      }
      service.dataBuildersOutputDirAction!!.execute(dataBuildersDirectoryConnection)
      generateApolloSources.configure {
        it.dependsOn(dataBuildersDirectoryConnection.task)
      }
    }

    generateApolloSources.configure {
      it.dependsOn(directoryConnection.task)
    }

    registerDownloadSchemaTasks(service)

    service.generateApolloMetadata.disallowChanges()
    service.registered = true
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
        connection.connectToAllAndroidVariants()
      }

      project.kotlinProjectExtension != null -> {
        connection.connectToKotlinSourceSet("main")
      }

      project.javaExtension != null -> {
        connection.connectToJavaSourceSet("main")
      }

      else -> throw IllegalStateException("Cannot find a Java/Kotlin extension, please apply the kotlin or java plugin")
    }
  }

  private val defaultDataBuildersOutputDirAction = Action<Service.DirectoryConnection> { connection ->
    when {
      project.kotlinMultiplatformExtension != null -> {
        connection.connectToKotlinSourceSet("commonTest")
      }

      project.androidExtension != null -> {
        connectToAllAndroidTestVariants(project, connection.outputDir, connection.task)
      }

      project.kotlinProjectExtension != null -> {
        connection.connectToKotlinSourceSet("test")
      }

      project.javaExtension != null -> {
        connection.connectToJavaSourceSet("test")
      }

      else -> throw IllegalStateException("Cannot find a Java/Kotlin extension, please apply the kotlin or java plugin")
    }
  }

  private fun registerDownloadSchemaTasks(service: DefaultService) {
    val introspection = service.introspection
    var taskProvider: TaskProvider<ApolloDownloadSchemaTask>? = null
    var connection: Action<SchemaConnection>? = null

    if (introspection != null) {
      taskProvider = project.registerApolloDownloadSchemaTask(
          taskName = ModelNames.downloadApolloSchemaIntrospection(service),
          taskGroup = TASK_GROUP,
          schema = project.provider { service.guessSchemaFile(project, introspection.schemaFile) },
          endpoint = introspection.endpointUrl,
          headers = introspection.headers,
          graph = project.provider { null },
          key = project.provider { null },
          graphVariant = project.provider { null },
          registryUrl = project.provider { null },
          insecure = project.provider { false }
      )
      connection = introspection.schemaConnection
    }
    val registry = service.registry
    if (registry != null) {
      taskProvider = project.registerApolloDownloadSchemaTask(
          taskName = ModelNames.downloadApolloSchemaRegistry(service),
          taskGroup = TASK_GROUP,
          schema = project.provider { service.guessSchemaFile(project, registry.schemaFile) },
          endpoint = project.provider { null },
          headers = project.objects.mapProperty(String::class.java, String::class.java),
          graph = registry.graph,
          key = registry.key,
          graphVariant = registry.graphVariant,
          registryUrl = project.provider { null },
          insecure = project.provider { false }
      )

      connection = registry.schemaConnection
    }
    if (connection != null && taskProvider != null) {
      connection.execute(
          SchemaConnection(
              taskProvider,
              taskProvider.flatMap { downloadSchemaTask ->
                project.layout.file(downloadSchemaTask.schema)
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

        variant.sourceSets.forEach { sourceProvider ->
          service.srcDir("src/${sourceProvider.name}/graphql/$sourceFolder")
        }
        (service as DefaultService).outputDirAction = Action<Service.DirectoryConnection> { connection ->
          connection.connectToAndroidVariant(variant.wrapped)
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

    // Keep in sync gradle-api-min
    const val MIN_GRADLE_VERSION = "8.0"

    private fun getDeps(configurations: ConfigurationContainer): List<String> {
      // See https://github.com/apollographql/apollo-kotlin/pull/5657
      val currentConfigurations = configurations.toList()
      return currentConfigurations.flatMap { configuration ->
        configuration.dependencies
            .filter {
              /**
               * When using plugins {}, the group is the plugin id, not the maven group
               *
               * the "_" check is for refreshVersions,
               * see https://github.com/jmfayard/refreshVersions/issues/507
               *
               * Note: we only check external dependencies as reading the group of project dependencies
               * is not compatible with isolated projects
               *
               */
              it is ExternalModuleDependency
                  && it.group in listOf("com.apollographql.apollo", "com.apollographql.apollo.external")
                  && it.version != "_"
            }.mapNotNull { dependency ->
              dependency.version
            }
      }
    }

    // Don't use `graphqlSourceDirectorySet.isEmpty` here, it doesn't work for some reason
    private val SourceDirectorySet.isReallyEmpty
      get() = sourceDirectories.isEmpty

    internal fun Project.hasJavaPlugin() = project.extensions.findByName("java") != null
    internal fun Project.hasKotlinPlugin() = project.extensions.findByName("kotlin") != null
  }

  override val deps: ApolloDependencies = ApolloDependencies(project.dependencies)
}


private fun Task.outputDirectory(): Provider<Directory> {
  return when (this) {
    is ApolloGenerateSourcesTask -> this.outputDirectory
    is ApolloGenerateSourcesFromIrTask -> this.outputDirectory
    else -> error("Unexpected task $this")
  }
}

private fun Task.operationManifest(): Provider<RegularFile> {
  return when (this) {
    is ApolloGenerateSourcesTask -> this.operationManifest
    is ApolloGenerateSourcesFromIrTask -> this.operationManifest
    else -> error("Unexpected task $this")
  }
}

private fun Task.dataBuildersOutputDirectory(): Provider<Directory> {
  return when (this) {
    is ApolloGenerateSourcesTask -> this.dataBuildersOutputDirectory
    is ApolloGenerateDataBuildersSourcesTask -> this.outputDirectory
    else -> error("Unexpected task $this")
  }
}
