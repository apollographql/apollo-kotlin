package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.compiler.OperationIdGenerator
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File
import java.util.concurrent.Callable

abstract class DefaultApolloExtension(
    private val project: Project,
    private val defaultService: DefaultService,
) : ApolloExtension, Service by defaultService {

  private val services = mutableListOf<DefaultService>()
  private val checkVersionsTask: TaskProvider<Task>
  private val apolloConfiguration: Configuration
  private val rootProvider: TaskProvider<Task>
  private var registerDefaultService = true

  // Called when the plugin is applied
  init {
    require(GradleVersion.current() >= GradleVersion.version(MIN_GRADLE_VERSION)) {
      "apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater"
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
        @Suppress("DEPRECATION")
        check(defaultService.graphqlSourceDirectorySet.isEmpty
            && defaultService.schemaFile.isPresent.not()
            && defaultService.schemaFiles.isEmpty
            && defaultService.alwaysGenerateTypesMatching.isPresent.not()
            && defaultService.customScalarsMapping.isPresent.not()
            && defaultService.customTypeMapping.isPresent.not()
            && defaultService.excludes.isPresent.not()
            && defaultService.includes.isPresent.not()
            && defaultService.failOnWarnings.isPresent.not()
            && defaultService.generateApolloMetadata.isPresent.not()
            && defaultService.generateAsInternal.isPresent.not()
            && defaultService.codegenModels.isPresent.not()
            && defaultService.generateFragmentImplementations.isPresent.not()
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
        allDeps.mapNotNull { it.version }.distinct().sorted()
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

  private fun registerService(service: DefaultService) {
    check(services.find { it.name == service.name } == null) {
      "There is already a service named $name, please use another name"
    }
    services.add(service)

    val producerConfigurationName = ModelNames.producerConfiguration(service)

    project.configurations.create(producerConfigurationName) {
      it.isCanBeConsumed = true
      it.isCanBeResolved = false

      /**
       * Expose transitive dependencies to downstream consumers
       */
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

    project.afterEvaluate {
      if (shouldGenerateMetadata(service)) {
        project.artifacts {
          it.add(producerConfigurationName, codegenProvider.flatMap { it.metadataOutputFile })
        }
      }
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
          project(mapOf("path" to project.path))
      )
    }

    codegenProvider.configure {
      it.finalizedBy(checkApolloDuplicates)
    }

    if (service.operationOutputAction != null) {
      val operationOutputConnection = Service.OperationOutputConnection(
          task = codegenProvider,
          operationOutputFile = codegenProvider.flatMap { it.operationOutputFile }
      )
      service.operationOutputAction!!.execute(operationOutputConnection)
    }

    if (service.outputDirAction == null) {
      service.outputDirAction = defaultOutputDirAction
    }
    service.outputDirAction!!.execute(
        DefaultDirectoryConnection(
            project = project,
            task = codegenProvider,
            outputDir = codegenProvider.flatMap { it.outputDir }
        )
    )

    if (service.testDirAction == null) {
      service.testDirAction = defaultTestDirAction
    }
    service.testDirAction!!.execute(
        DefaultDirectoryConnection(
            project = project,
            task = codegenProvider,
            outputDir = codegenProvider.flatMap { it.testDir }
        )
    )

    rootProvider.configure {
      it.dependsOn(codegenProvider)
    }

    registerDownloadSchemaTasks(service)
    maybeRegisterRegisterOperationsTasks(project, service, codegenProvider)
  }

  private fun maybeRegisterRegisterOperationsTasks(project: Project, service: DefaultService, codegenProvider: TaskProvider<ApolloGenerateSourcesTask>) {
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
        || apolloConfiguration.dependencies.isNotEmpty()
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
      consumerConfiguration: Configuration,
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
        getKotlinTargetLanguage(project, languageVersion.orNull)
      } else {
        TargetLanguage.JAVA
      }

      task.useSemanticNaming.set(service.useSemanticNaming)
      task.targetLanguage.set(targetLanguage)
      task.warnOnDeprecatedUsages.set(service.warnOnDeprecatedUsages)
      task.failOnWarnings.set(service.failOnWarnings)
      @Suppress("DEPRECATION")
      task.customScalarsMapping.set(service.customScalarsMapping.orElse(service.customTypeMapping))
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

      task.metadataFiles.from(consumerConfiguration)

      check(!(service.packageName.isPresent && service.packageNameGenerator.isPresent)) {
        "Apollo: it is an error to specify both 'packageName' and 'packageNameGenerator' " +
            "(either directly or indirectly through useVersion2Compat())"
      }
      var packageNameGenerator = service.packageNameGenerator.orNull
      if (packageNameGenerator == null) {
        packageNameGenerator = PackageNameGenerator.Flat(service.packageName.orNull ?: error("""
            |Apollo: specify 'packageName':
            |apollo {
            |  packageName.set("com.example")
            |  
            |  // Alternatively, if you're migrating from 2.x, you can keep the 2.x   
            |  // behaviour with `packageNamesFromFilePaths()`: 
            |  packageNamesFromFilePaths()
            |}
          """.trimMargin()))
      }
      task.packageNameGenerator = packageNameGenerator
      task.generateAsInternal.set(service.generateAsInternal)
      task.generateFilterNotNull.set(project.isKotlinMultiplatform)
      task.alwaysGenerateTypesMatching.set(service.alwaysGenerateTypesMatching)
      task.projectName.set(project.name)
      task.generateFragmentImplementations.set(service.generateFragmentImplementations)
      task.generateQueryDocument.set(service.generateQueryDocument)
      task.generateSchema.set(service.generateSchema)
      task.codegenModels.set(service.codegenModels)
      task.flattenModels.set(service.flattenModels)
      @OptIn(ApolloExperimental::class)
      task.generateTestBuilders.set(service.generateTestBuilders)
      task.sealedClassesForEnumsMatching.set(service.sealedClassesForEnumsMatching)
      task.generateOptionalOperationVariables.set(service.generateOptionalOperationVariables)
      task.languageVersion.set(service.languageVersion)
    }
  }

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
        task.endpoint.set(introspection.endpointUrl)
        task.header = introspection.headers.get().map { "${it.key}: ${it.value}" }
        task.schema.set(project.provider { lazySchemaFileForDownload(service, introspection.schemaFile) })
      }
    }
    val registry = service.registry
    if (registry != null) {
      project.tasks.register(ModelNames.downloadApolloSchemaRegistry(service), ApolloDownloadSchemaTask::class.java) { task ->

        task.group = TASK_GROUP
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

  companion object {
    private const val TASK_GROUP = "apollo"
    const val MIN_GRADLE_VERSION = "5.6"

    private const val USAGE_APOLLO_METADATA = "apollo-metadata"

    private data class Dep(val name: String, val version: String?)

    private fun getDeps(configurations: ConfigurationContainer): List<Dep> {
      return configurations.flatMap { configuration ->
        configuration.dependencies
            .filter {
              it.group == "com.apollographql.apollo3"
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
        srcDir.walkTopDown().filter { it.extension in listOf("json", "sdl", "graphqls") }.toList()
      }.toSet()
    }
  }
}
