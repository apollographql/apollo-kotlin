package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.AgpCompat
import com.apollographql.apollo.gradle.ComponentFilter
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
import com.apollographql.apollo.gradle.task.registerApolloGenerateCompilationUnitModelTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateDataBuildersSourcesTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateIrOperationsTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateOptionsTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateProjectModelTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateSourcesFromIrTask
import com.apollographql.apollo.gradle.task.registerApolloGenerateSourcesTask
import com.apollographql.apollo.gradle.task.registerApolloRegisterOperationsTask
import com.apollographql.apollo.gradle.Agp8
import com.apollographql.apollo.gradle.Agp8Component
import com.apollographql.apollo.gradle.Agp9
import com.apollographql.apollo.gradle.Agp9Component
import gratatouille.wiring.capitalizeFirstLetter
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import java.io.File
import javax.inject.Inject

abstract class DefaultApolloExtension(
    private val project: Project,
) : ApolloExtension {

  private var codegenOnGradleSyncConfigured: Boolean = false
  private val services = mutableListOf<DefaultService>()
  private val generateApolloSources: TaskProvider<Task>
  private val generateApolloProjectModel: TaskProvider<out Task>
  private var hasExplicitService = false
  private val adhocComponentWithVariants: AdhocComponentWithVariants by lazy {
    softwareComponentFactory.adhoc("apollo").also {
      project.components.add(it)
    }
  }

  /**
   * Needs to be lazy because we don't know the order in which the plugins are going to be applied
   */
  internal val agpOrNull: AgpCompat? by lazy {
    val androidComponents = project.extensions.findByName("androidComponents")
    if (androidComponents != null) {
      val agpVersion = agpVersion()
      val major = agpVersion.split('.').get(0).toIntOrNull()
      check(major != null) {
        "Apollo: unrecognized AGP version: $agpVersion"
      }
      if (major >= 9) {
        Agp9(agpVersion, androidComponents, project.extensions.findByName("android"), project.extensions.findByName("kotlin"))
      } else {
        Agp8(agpVersion, project.extensions.findByName("android") ?: error("No 'android' extension found. If you're applying `com.android.kotlin.multiplatform.library`, Apollo only supports it in conjunction with AGP9."))
      }
    } else {
      null
    }
  }

  internal val agp
    get() = agpOrNull ?: error("Apollo: androidComponents extension not found, is the Android Gradle Plugin applied?")

  internal fun getServiceInfos(project: Project): List<ApolloGradleToolingModel.ServiceInfo> = services.map { service ->
    DefaultServiceInfo(
        name = service.name,
        schemaFiles = service.schemaFilesSnapshot(project),
        graphqlSrcDirs = service.graphqlSourceDirectorySet.srcDirs,
        upstreamProjects = service.upstreamScope.get().dependencies.filterIsInstance<ProjectDependency>().map { it.name }.toSet(),
        upstreamProjectPaths = service.upstreamScope.get().dependencies.filterIsInstance<ProjectDependency>().map { it.getPathCompat() }.toSet(),
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
        generateApolloMetadata = service.generateApolloMetadata.orNull,

        // Options for which we don't mind the value but want to know they are used
        usedOptions = service.telemetryUsedOptions(),
    )
  }

  private fun DefaultService.telemetryUsedOptions(): Set<String> = mutableSetOf<String>().apply {
    if (includes.isPresent) add("includes")
    if (excludes.isPresent) add("excludes")
    if (schemaFile.isPresent) add("schemaFile")
    if (!schemaFiles.isEmpty) add("schemaFiles")
    if (scalarAdapterMapping.isNotEmpty()) {
      add("mapScalarAdapterExpression")
    } else if (scalarTypeMapping.isNotEmpty()) {
      add("mapScalar")
    }
    @Suppress("DEPRECATION_ERROR")
    if (operationManifest.isPresent) add("operationManifest")
    if (generatedSchemaName.isPresent) add("generatedSchemaName")
    if (debugDir.isPresent) add("debugDir")
    if (sealedClassesForEnumsMatching.isPresent) add("sealedClassesForEnumsMatching")
    if (classesForEnumsMatching.isPresent) add("classesForEnumsMatching")
    @Suppress("DEPRECATION_ERROR")
    if (outputDir.isPresent) add("outputDir")
    if (alwaysGenerateTypesMatching.isPresent) add("alwaysGenerateTypesMatching")
    if (introspection != null) add("introspection")
    if (registry != null) add("registry")
    if (upstreamScope.get().dependencies.isNotEmpty()) add("dependsOn")
    if (downstreamScope.get().dependencies.isNotEmpty()) add("isADependencyOf")
    @Suppress("DEPRECATION")
    if (warnOnDeprecatedUsages.isPresent) add("warnOnDeprecatedUsages")
    @Suppress("DEPRECATION")
    if (fieldsOnDisjointTypesMustMerge.isPresent) add("fieldsOnDisjointTypesMustMerge")
    if (issueSeverities.isNotEmpty()) add("issueSeverities")
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

    /**
     * Project model for integration with other tools, e.g. IDE plugin.
     */
    generateApolloProjectModel = project.registerApolloGenerateProjectModelTask(
        taskName = ModelNames.generateApolloProjectModel(),
        taskDescription = "Generate Apollo project model",

        serviceNames = project.provider { services.map { it.name }.toSet() },
        apolloTasksDependencies = project.provider {
          project.configurations.getByName("apolloTasks").files.map { it.absolutePath }.toSet()
        },

        // Telemetry
        gradleVersion = project.provider { project.gradle.gradleVersion },
        androidMinSdk = project.provider { agpOrNull?.minSdk() },
        androidTargetSdk = project.provider { agpOrNull?.targetSdk() },
        androidCompileSdk = project.provider { agpOrNull?.compileSdk() },
        androidAgpVersion = project.provider { agpOrNull?.version },
        apolloGenerateSourcesDuringGradleSync = generateSourcesDuringGradleSync,
        apolloLinkSqlite = linkSqlite,
        usedServiceOptions = project.provider { services.flatMap { it.telemetryUsedOptions() }.toSet() }
    )

    project.afterEvaluate {
      maybeLinkSqlite()
      checkForLegacyJsTarget()
    }
  }

  private fun checkForLegacyJsTarget() {
    val kotlin = project.extensions.findByName("kotlin") as? KotlinMultiplatformExtension
    val hasLegacyJsTarget = kotlin?.targets?.any { target ->
      target is KotlinJsTarget && target.irTarget == null
    } == true
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
    project.afterEvaluate {
      if (!service.upstreamScope.get().dependencies.isEmpty()) {
        check(service.scalarTypeMapping.isEmpty()) {
          "Apollo: the custom scalar configuration is not used in non-schema modules. Add custom scalars to your schema module."
        }
        check(!service.generateDataBuilders.isPresent) {
          "Apollo: generateDataBuilders is not used in non-schema modules. Add generateDataBuilders to your schema module."
        }
      }
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

  class Configurations(
      val consumable: NamedDomainObjectProvider<Configuration>,
      val resolvable: NamedDomainObjectProvider<Configuration>,
  )

  private fun createConfigurations(
      serviceName: String,
      apolloUsage: ApolloUsage,
      direction: ApolloDirection,
      extendsFrom: NamedDomainObjectProvider<Configuration>,
  ): Configurations {
    val consumable =
      project.configurations.register(ModelNames.configuration(serviceName, direction, apolloUsage, ConfigurationKind.Consumable)) {
        it.isCanBeConsumed = true
        it.isCanBeResolved = false

        it.extendsFrom(extendsFrom.get())
        it.attributes(serviceName, apolloUsage, direction)
      }
    val resolvable =
      project.configurations.register(ModelNames.configuration(serviceName, direction, apolloUsage, ConfigurationKind.Resolvable)) {
        it.isCanBeConsumed = false
        it.isCanBeResolved = true

        /**
         * By default all apollo configurations are transitive
         *
         * If you have a graph like this:
         *
         * ```kotlin
         * // leaf/build.gradle.kts
         * dependencies {
         *   add("apolloService", project(":intermediate"))
         * }
         *
         * // leaf/build.gradle.kts
         * dependencies {
         *   add("apolloService", project(":schema"))
         * }
         *
         * // schema/build.gradle.kts
         * apollo {
         *   service("service") {
         *     generateApolloMetadata.set(true)
         *   }
         * }
         * ```
         *
         * Then `apolloServiceCodegenSchemaResolvable` contains the codegen schemas for both `leaf` and `schema` projects.
         *
         * This also means the fragments defined in `schema` are available in `leaf` even though there is not a direct dependency.
         *
         * In other terms, Apollo doesn't have a concept of `api` vs `implementation`.
         */
        it.isTransitive = true

        it.extendsFrom(extendsFrom.get())
        it.attributes(serviceName, apolloUsage, direction)
      }

    return Configurations(
        consumable = consumable,
        resolvable = resolvable
    )
  }

  private fun <T : Any> HasConfigurableAttributes<T>.attributes(serviceName: String, usage: ApolloUsage, direction: ApolloDirection) {
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

    val otherOptions = createConfigurations(
        serviceName = service.name,
        apolloUsage = ApolloUsage.OtherOptions,
        direction = ApolloDirection.Upstream,
        extendsFrom = service.upstreamScope
    )

    val warnIfNoPluginFound = service.hasPlugin
    val pluginArguments = project.provider {
      @Suppress("USELESS_CAST")
      service.pluginsArguments as Map<String, Any?>
    }

    if (service.languageVersion.orNull == "1.5") {
      project.logger.lifecycle("Apollo: languageVersion 1.5 is deprecated, please use 1.9 or leave empty")
    }
    @Suppress("DEPRECATION")
    val fieldsOnDisjointTypesMustMerge = service.fieldsOnDisjointTypesMustMerge.orNull
    if (fieldsOnDisjointTypesMustMerge != null) {
      service.issueSeverities.put("DifferentShape", if (fieldsOnDisjointTypesMustMerge) "error" else "ignore")
    }
    @Suppress("DEPRECATION")
    val warnOnDeprecatedUsages = service.warnOnDeprecatedUsages.orNull
    if (warnOnDeprecatedUsages != null) {
      service.issueSeverities.put("DeprecatedUsage", if (warnOnDeprecatedUsages) "warn" else "ignore")
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
        decapitalizeFields = service.decapitalizeFields,
        flattenModels = service.flattenModels,
        severities = project.provider { service.issueSeverities },
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
        generateApolloEnums = service.generateApolloEnums,
        generateAsInternal = service.generateAsInternal,
        generateInputBuilders = service.generateInputBuilders,
        addJvmOverloads = service.addJvmOverloads,
        requiresOptInAnnotation = service.requiresOptInAnnotation,
        jsExport = service.jsExport,

        /**
         * Gradle model
         */
        upstreamOtherOptions = project.files(otherOptions.resolvable),
        javaPluginApplied = project.provider { project.hasJavaPlugin() },
        kgpVersion = project.provider { project.apolloGetKotlinPluginVersion() },
        kmp = project.provider { project.isKotlinMultiplatform },
        // If there is no downstream dependency, generate everything because we don't know what types are going to be used downstream
        generateAllTypes = project.provider { service.isSchemaModule() && service.isMultiModule() && service.downstreamScope.get().dependencies.isEmpty() },
    )
    generateApolloProjectModel.configure {
      it.dependsOn(optionsTaskProvider)
    }

    val compilationUnitModelTaskProvider = project.registerApolloGenerateCompilationUnitModelTask(
        taskName = ModelNames.generateApolloCompilationUnitModel(service),
        taskDescription = "Generate Apollo compilation unit model for '${service.name}'",

        gradleProjectPath = project.provider { project.path },
        serviceName = project.provider { service.name },
        schemaFiles = project.provider { service.schemaFilesSnapshot(project).map { it.absolutePath }.toSet() },
        graphqlSrcDirs = project.provider { service.graphqlSourceDirectorySet.srcDirs.map { it.absolutePath }.toSet() },
        upstreamGradleProjectPaths = project.provider {
          service.upstreamScope.get().dependencies.filterIsInstance<ProjectDependency>().map { it.getPathCompat() }.toSet()
        },
        downstreamGradleProjectPaths = project.provider {
          service.downstreamScope.get().dependencies.filterIsInstance<ProjectDependency>().map { it.getPathCompat() }.toSet()
        },
        endpointUrl = project.provider { service.introspection?.endpointUrl?.orNull },
        endpointHeaders = project.provider { service.introspection?.headers?.orNull },
        pluginDependencies = service.compilerConfiguration.map { configuration ->
          configuration.files.map { it.absolutePath }.toSet()
        },
        pluginArguments = pluginArguments,
    )
    generateApolloProjectModel.configure {
      it.dependsOn(compilationUnitModelTaskProvider)
    }

    if (!service.isMultiModule()) {
      sourcesBaseTaskProvider = project.registerApolloGenerateSourcesTask(
          taskName = ModelNames.generateApolloSources(service),
          taskGroup = TASK_GROUP,
          taskDescription = "Generate Apollo models for service '${service.name}'",
          extraClasspath = project.files(service.compilerConfiguration),
          arguments = pluginArguments,
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
          extendsFrom = service.upstreamScope
      )

      val upstreamIr = createConfigurations(
          serviceName = service.name,
          apolloUsage = ApolloUsage.Ir,
          direction = ApolloDirection.Upstream,
          extendsFrom = service.upstreamScope
      )

      val downstreamIr = createConfigurations(
          serviceName = service.name,
          apolloUsage = ApolloUsage.Ir,
          direction = ApolloDirection.Downstream,
          extendsFrom = service.downstreamScope
      )

      val codegenMetadata = createConfigurations(
          serviceName = service.name,
          apolloUsage = ApolloUsage.CodegenMetadata,
          direction = ApolloDirection.Upstream,
          extendsFrom = service.upstreamScope
      )

      /**
       * Tasks
       */
      val codegenSchemaTaskProvider = project.registerApolloGenerateCodegenSchemaTask(
          taskName = ModelNames.generateApolloCodegenSchema(service),
          taskGroup = TASK_GROUP,
          taskDescription = "Generate Apollo schema for service '${service.name}'",
          schemaFiles = service.schemaFiles(project),
          fallbackSchemaFiles = service.fallbackSchemaFiles(project),
          upstreamSchemaFiles = project.files(codegenSchema.resolvable),
          codegenSchemaOptionsFile = optionsTaskProvider.flatMap { it.codegenSchemaOptionsFile },
          arguments = pluginArguments,
          extraClasspath = project.files(service.compilerConfiguration),
          warnIfNotFound = project.provider { warnIfNoPluginFound },
      )

      val upstreamAndSelfCodegenSchemas = project.files().also {
        it.from(codegenSchema.resolvable)
        it.from(codegenSchemaTaskProvider.flatMap { it.codegenSchemaFile })
      }
      val irOperationsTaskProvider = project.registerApolloGenerateIrOperationsTask(
          taskName = ModelNames.generateApolloIrOperations(service),
          taskGroup = TASK_GROUP,
          taskDescription = "Generate Apollo IR operations for service '${service.name}'",
          extraClasspath = project.files(service.compilerConfiguration),
          arguments = pluginArguments,
          warnIfNotFound = project.provider { warnIfNoPluginFound },
          upstreamIrFiles = project.files(upstreamIr.resolvable),
          codegenSchemas = upstreamAndSelfCodegenSchemas,
          graphqlFiles = service.graphqlSourceDirectorySet,
          irOptionsFile = optionsTaskProvider.flatMap { it.irOptionsFile },
      )

      val computeUsedCoordinatesTask = project.registerApolloComputeUsedCoordinatesTask(
          taskName = ModelNames.computeUsedCoordinates(service),
          taskGroup = TASK_GROUP,
          irOperations = project.files(downstreamIr.resolvable),
      )
      val sourcesFromIrTaskProvider = project.registerApolloGenerateSourcesFromIrTask(
          taskName = ModelNames.generateApolloSources(service),
          taskGroup = TASK_GROUP,
          taskDescription = "Generate Apollo models for service '${service.name}'",
          extraClasspath = project.files(service.compilerConfiguration),
          arguments = pluginArguments,
          warnIfNotFound = project.provider { warnIfNoPluginFound },
          codegenSchemas = upstreamAndSelfCodegenSchemas,
          downstreamUsedCoordinates = computeUsedCoordinatesTask.flatMap { it.outputFile },
          irOperations = irOperationsTaskProvider.flatMap { it.irOperationsFile },
          upstreamMetadata = project.files(codegenMetadata.resolvable),
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
            extraClasspath = project.files(service.compilerConfiguration),
            arguments = pluginArguments,
            warnIfNotFound = project.provider { warnIfNoPluginFound },
            codegenSchemas = upstreamAndSelfCodegenSchemas,
            downstreamUsedCoordinates = computeUsedCoordinatesTask.flatMap { it.outputFile },
            irOperations = irOperationsTaskProvider.flatMap { it.irOperationsFile },
            upstreamMetadata = upstreamAndSelfCodegenMetadata,
            codegenOptions = optionsTaskProvider.flatMap { it.codegenOptions },
            outputDirectory = dataBuildersOutputDir(project, service)
        )
      } else {
        null
      }

      project.artifacts {
        it.add(codegenSchema.consumable.name, codegenSchemaTaskProvider.flatMap { it.codegenSchemaFile }) {
          it.classifier = "codegen-schema-${service.name}"
        }
        it.add(otherOptions.consumable.name, optionsTaskProvider.flatMap { it.otherOptions }) {
          it.classifier = "other-options-${service.name}"
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
          get() = listOf(codegenMetadata.consumable.get(), upstreamIr.consumable.get(), codegenSchema.consumable.get(), otherOptions.consumable.get())
      }
      if (service.outgoingVariantsConnection != null) {
        service.outgoingVariantsConnection!!.execute(outgoingVariantsConnection)
      } else {
        outgoingVariantsConnection.addToSoftwareComponent(adhocComponentWithVariants)
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
        agp = agpOrNull,
        task = sourcesBaseTaskProvider,
        outputDir = sourcesBaseTaskProvider.flatMap { it.outputDirectory() },
        hardCodedOutputDir = outputDir(project, service),
        wiredWith = { it.outputDirectory() }
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

    if (dataBuildersSourcesBaseTaskProvider != null) {
      val dataBuildersDirectoryConnection = DefaultDirectoryConnection(
          project = project,
          agp = agpOrNull,
          task = dataBuildersSourcesBaseTaskProvider,
          outputDir = dataBuildersSourcesBaseTaskProvider.flatMap {
            it.dataBuildersOutputDirectory()
          },
          hardCodedOutputDir = dataBuildersOutputDir(project, service),
          wiredWith = { it.dataBuildersOutputDirectory() }
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

      agpOrNull != null -> {
        connection.connectToAndroidVariants(project.extensions.findByName("kotlin") != null)
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

      agpOrNull != null -> {
        connection.connectToAndroidTestComponents(project.extensions.findByName("kotlin") != null)
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

  private fun Project.regularFileProperty(provider: () -> File): RegularFileProperty {
    return objects.fileProperty().fileProvider(project.provider(provider))
  }

  private fun registerDownloadSchemaTasks(service: DefaultService) {
    val introspection = service.introspection
    var taskProvider: TaskProvider<ApolloDownloadSchemaTask>? = null
    var connection: Action<SchemaConnection>? = null

    if (introspection != null) {
      taskProvider = project.registerApolloDownloadSchemaTask(
          taskName = ModelNames.downloadApolloSchemaIntrospection(service),
          taskGroup = TASK_GROUP,
          schema = project.regularFileProperty { service.guessSchemaFile(project, introspection.schemaFile) },
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
          schema = project.regularFileProperty { service.guessSchemaFile(project, registry.schemaFile) },
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
              taskProvider.flatMap { it.schema }
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

    agp.onComponent(ComponentFilter.All) { component ->
      val name = "${component.name}${nameSuffix.capitalizeFirstLetter()}"

      service(name) { service ->
        action.execute(service)

        if (component is Agp8Component) {
          component.sourceSets.forEach {
            service.srcDir("src/${it}/graphql/$sourceFolder")
          }
        } else if (component is Agp9Component) {
          component.graphQLDirectories.forEach {
            service.srcDir(it)
          }
        }
        (service as DefaultService).outputDirAction = Action<Service.DirectoryConnection> { connection ->
          connection.connectToAndroidComponent(component.wrappedComponent, project.extensions.findByName("kotlin") != null)
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

    // Don't use `graphqlSourceDirectorySet.isEmpty` here, it doesn't work for some reason
    private val SourceDirectorySet.isReallyEmpty
      get() = sourceDirectories.isEmpty

    internal fun Project.hasJavaPlugin() = project.extensions.findByName("java") != null
    internal fun Project.hasKotlinPlugin() = project.extensions.findByName("kotlin") != null
  }

  override val deps: ApolloDependencies = ApolloDependencies(project.dependencies)
}


private fun Task.outputDirectory(): DirectoryProperty {
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

private fun Task.dataBuildersOutputDirectory(): DirectoryProperty {
  return when (this) {
    is ApolloGenerateSourcesTask -> this.dataBuildersOutputDirectory
    is ApolloGenerateDataBuildersSourcesTask -> this.outputDirectory
    else -> error("Unexpected task $this")
  }
}
