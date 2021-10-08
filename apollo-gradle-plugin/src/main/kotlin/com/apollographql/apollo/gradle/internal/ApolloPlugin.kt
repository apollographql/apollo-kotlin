package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.OperationOutputGenerator
import com.apollographql.apollo.gradle.api.ApolloAttributes
import com.apollographql.apollo.gradle.api.ApolloExtension
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import java.net.URLDecoder

open class ApolloPlugin : Plugin<Project> {
  internal companion object {
    const val TASK_GROUP = "apollo"
    const val MIN_GRADLE_VERSION = "5.6"

    const val CONFIGURATION_CONSUMER = "apollo"
    const val USAGE_APOLLO_METADATA = "apollo-metadata"

    val Project.isKotlinMultiplatform get() = pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")

    private fun registerCompilationUnits(project: Project, apolloExtension: DefaultApolloExtension, checkVersionsTask: TaskProvider<Task>) {
      val androidExtension = project.extensions.findByName("android")

      val apolloConfiguration = project.configurations.getByName(ModelNames.apolloConfiguration())

      val apolloVariants = when {
        project.isKotlinMultiplatform -> KotlinMultiplatformTaskConfigurator.getVariants(project)
        androidExtension != null -> AndroidTaskConfigurator.getVariants(project, androidExtension)
        else -> JvmTaskConfigurator.getVariants(project)
      }

      val rootProvider = project.tasks.register(ModelNames.generateApolloSources()) {
        it.group = TASK_GROUP
        it.description = "Generate Apollo models for all services and variants"
      }

      val services = if (apolloExtension.services.isEmpty()) {
        listOf(project.objects.newInstance(DefaultService::class.java, project.objects, "service"))
      } else {
        apolloExtension.services
      }

      apolloVariants.all { apolloVariant ->
        val variantProvider = project.tasks.register(ModelNames.generateApolloSources(apolloVariant)) {
          it.group = TASK_GROUP
          it.description = "Generate Apollo models for all services and variant '${apolloVariant.name}'"
        }

        val compilationUnits = services.map {
          DefaultCompilationUnit.createDefaultCompilationUnit(project, apolloExtension, apolloVariant, it)
        }

        compilationUnits.forEach { compilationUnit ->
          val producerConfigurationName = ModelNames.producerConfiguration(compilationUnit)
          project.configurations.create(producerConfigurationName) {
            it.isCanBeConsumed = true
            it.isCanBeResolved = false

            it.extendsFrom(apolloConfiguration)

            it.attributes {
              it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
              it.attribute(ApolloAttributes.APOLLO_VARIANT_ATTRIBUTE, project.objects.named(ApolloAttributes.Variant::class.java, compilationUnit.variantName))
              it.attribute(ApolloAttributes.APOLLO_SERVICE_ATTRIBUTE, project.objects.named(ApolloAttributes.Service::class.java, compilationUnit.serviceName))
            }
          }

          val consumerConfiguration = project.configurations.create(ModelNames.consumerConfiguration(compilationUnit)) {
            it.isCanBeResolved = true
            it.isCanBeConsumed = false

            it.extendsFrom(apolloConfiguration)

            it.attributes {
              it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
              it.attribute(ApolloAttributes.APOLLO_VARIANT_ATTRIBUTE, project.objects.named(ApolloAttributes.Variant::class.java, compilationUnit.variantName))
              it.attribute(ApolloAttributes.APOLLO_SERVICE_ATTRIBUTE, project.objects.named(ApolloAttributes.Service::class.java, compilationUnit.serviceName))
            }
          }

          val codegenProvider = registerCodeGenTask(project, compilationUnit, consumerConfiguration)

          project.artifacts {
            it.add(producerConfigurationName, codegenProvider.flatMap { it.metadataOutputFile })
          }

          codegenProvider.configure {
            it.dependsOn(checkVersionsTask)
            it.dependsOn(consumerConfiguration)
          }

          variantProvider.configure {
            it.dependsOn(codegenProvider)
          }

          val checkApolloDuplicates = maybeRegisterCheckDuplicates(project.rootProject, compilationUnit)

          // Add project dependency on root project to this project, with our new configurations
          project.rootProject.dependencies.apply {
            add(
                ModelNames.duplicatesConsumerConfiguration(compilationUnit),
                project(mapOf("path" to project.path, "configuration" to producerConfigurationName))
            )
          }

          codegenProvider.configure {
            it.finalizedBy(checkApolloDuplicates)
          }

          compilationUnit.outputDir.set(codegenProvider.map { it.outputDir.get() })
          compilationUnit.operationOutputFile.set(codegenProvider.flatMap { it.operationOutputFile })

          /**
           * Order matters here. See https://github.com/apollographql/apollo-android/issues/1970
           * We want to expose the `CompilationUnit` to users before the task is configured by
           * AndroidTaskConfigurator.registerGeneratedDirectory so that schemaFile and sourceDirectorySet are
           * correctly set
           */
          apolloExtension.compilationUnits.add(compilationUnit)

          when {
            project.isKotlinMultiplatform -> {
              KotlinMultiplatformTaskConfigurator.registerGeneratedDirectory(project, compilationUnit, codegenProvider)
            }
            androidExtension != null -> AndroidTaskConfigurator.registerGeneratedDirectory(project, compilationUnit, codegenProvider)
            else -> JvmTaskConfigurator.registerGeneratedDirectory(project, compilationUnit, codegenProvider)
          }

          maybeRegisterRegisterOperationsTasks(project, compilationUnit, codegenProvider)

        }

        rootProvider.configure {
          it.dependsOn(variantProvider)
        }
      }
    }

    private fun maybeRegisterCheckDuplicates(rootProject: Project, compilationUnit: DefaultCompilationUnit): TaskProvider<ApolloCheckDuplicatesTask> {
      val taskName = ModelNames.checkApolloDuplicates(compilationUnit)
      return try {
        rootProject.tasks.named(taskName) as TaskProvider<ApolloCheckDuplicatesTask>
      } catch (e: Exception) {
        val configuration = rootProject.configurations.create(ModelNames.duplicatesConsumerConfiguration(compilationUnit)) {
          it.isCanBeResolved = true
          it.isCanBeConsumed = false

          it.attributes {
            it.attribute(Usage.USAGE_ATTRIBUTE, rootProject.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
            it.attribute(ApolloAttributes.APOLLO_VARIANT_ATTRIBUTE, rootProject.objects.named(ApolloAttributes.Variant::class.java, compilationUnit.variantName))
            it.attribute(ApolloAttributes.APOLLO_SERVICE_ATTRIBUTE, rootProject.objects.named(ApolloAttributes.Service::class.java, compilationUnit.serviceName))
          }
        }

        rootProject.tasks.register(taskName, ApolloCheckDuplicatesTask::class.java) {
          it.outputFile.set(BuildDirLayout.duplicatesCheck(rootProject, compilationUnit))
          it.metadataFiles.from(configuration)
        }
      }
    }

    private fun registerCodeGenTask(project: Project, compilationUnit: DefaultCompilationUnit, consumerConfiguration: Configuration): TaskProvider<ApolloGenerateSourcesTask> {
      return project.tasks.register(ModelNames.generateApolloSources(compilationUnit), ApolloGenerateSourcesTask::class.java) { task ->
        task.group = TASK_GROUP
        task.description = "Generate Apollo models for ${compilationUnit.name} GraphQL queries"

        val (compilerParams, graphqlSourceDirectorySet) = compilationUnit.resolveParams(project)

        task.graphqlFiles.setFrom(graphqlSourceDirectorySet)
        // I'm not sure if gradle is sensitive to the order of the rootFolders. Sort them just in case.
        task.rootFolders.set(project.provider { graphqlSourceDirectorySet.srcDirs.map { it.relativeTo(project.projectDir).path }.sorted() })
        task.schemaFile.set(compilerParams.schemaFile)
        task.operationOutputGenerator = compilerParams.operationOutputGenerator.getOrElse(
            OperationOutputGenerator.DefaultOperationOuputGenerator(
                compilerParams.operationIdGenerator.orElse(OperationIdGenerator.Sha256()).get()
            )
        )

        task.nullableValueType.set(compilerParams.nullableValueType)
        task.useSemanticNaming.set(compilerParams.useSemanticNaming)
        task.generateModelBuilder.set(compilerParams.generateModelBuilder)
        task.useJavaBeansSemanticNaming.set(compilerParams.useJavaBeansSemanticNaming)
        task.suppressRawTypesWarning.set(compilerParams.suppressRawTypesWarning)
        task.generateKotlinModels.set(compilationUnit.generateKotlinModels())
        task.warnOnDeprecatedUsages.set(compilerParams.warnOnDeprecatedUsages)
        task.failOnWarnings.set(compilerParams.failOnWarnings)
        task.generateVisitorForPolymorphicDatatypes.set(compilerParams.generateVisitorForPolymorphicDatatypes)
        task.customTypeMapping.set(compilerParams.customTypeMapping)
        task.outputDir.apply {
          set(BuildDirLayout.sources(project, compilationUnit))
          disallowChanges()
        }
        if (compilerParams.generateOperationOutput.getOrElse(false)) {
          task.operationOutputFile.apply {
            set(BuildDirLayout.operationOuput(project, compilationUnit))
            disallowChanges()
          }
        }
        // always set `metadataOutputFile` as the `metadata` task is part of `assemble` (see https://github.com/gradle/gradle/issues/14065)
        // and we don't want it to fail if it is ever called by the user
        task.metadataOutputFile.apply {
          set(BuildDirLayout.metadata(project, compilationUnit))
          disallowChanges()
        }

        task.generateMetadata.set(compilerParams.generateApolloMetadata.orElse(project.provider { !consumerConfiguration.isEmpty }))
        task.metadataFiles.from(consumerConfiguration)

        task.rootPackageName.set(compilerParams.rootPackageName)
        task.generateAsInternal.set(compilerParams.generateAsInternal)
        task.kotlinMultiPlatformProject.set(project.isKotlinMultiplatform)
        task.sealedClassesForEnumsMatching.set(compilerParams.sealedClassesForEnumsMatching)
        task.alwaysGenerateTypesMatching.set(compilerParams.alwaysGenerateTypesMatching)
        task.packageName.set(compilerParams.packageName.orNull)
        task.projectName.set(project.name)
        task.projectRootDir.set(project.rootProject.rootDir)
      }
    }

    private fun registerDownloadSchemaTasks(project: Project, apolloExtension: DefaultApolloExtension) {
      apolloExtension.services.forEach { service ->
        val introspection = service.introspection
        if (introspection != null) {
          project.tasks.register(ModelNames.downloadApolloSchema(service), ApolloDownloadSchemaTask::class.java) { task ->

            val sourceSetName = introspection.sourceSetName.orElse("main")
            task.group = TASK_GROUP
            task.schemaRelativeToProject.set(
                service.schemaPath.map {
                  "src/${sourceSetName.get()}/graphql/$it"
                }
            )

            task.endpoint.set(introspection.endpointUrl.map {
              it.toHttpUrl().newBuilder()
                  .apply {
                    introspection.queryParameters.get().entries.forEach {
                      addQueryParameter(it.key, it.value)
                    }
                  }
                  .build()
                  .toString()
            }
            )
            task.header = introspection.headers.get().map {
              "${it.key}: ${it.value}"
            }
          }
        }
      }

      project.tasks.register(ModelNames.downloadApolloSchema(), ApolloDownloadSchemaCliTask::class.java) { task ->
        task.group = TASK_GROUP
        task.compilationUnits = apolloExtension.compilationUnits
      }
      project.tasks.register(ModelNames.pushApolloSchema(), ApolloPushSchemaTask::class.java) { task ->
        task.group = TASK_GROUP
      }
    }

    private fun maybeRegisterRegisterOperationsTasks(project: Project, compilationUnit: DefaultCompilationUnit, codegenProvider: TaskProvider<ApolloGenerateSourcesTask>) {
      val registerOperationsConfig = compilationUnit.service.registerOperationsConfig
      if (registerOperationsConfig != null) {
          project.tasks.register(ModelNames.registerOperations(compilationUnit), ApolloRegisterOperationsTask::class.java) { task ->
            task.group = TASK_GROUP

            task.graph.set(registerOperationsConfig.graph)
            task.graphVariant.set(registerOperationsConfig.graphVariant)
            task.key.set(registerOperationsConfig.key)
            task.operationOutput.set(codegenProvider.flatMap { it.operationOutputFile })
          }
        }
    }

    fun toMap(s: String): Map<String, String> {
      return s.split("&")
          .map {
            val keyValue = it.split("=")
            val key = URLDecoder.decode(keyValue[0], "UTF-8")
            val value = URLDecoder.decode(keyValue[1], "UTF-8")

            key to value
          }.toMap()
    }

    private fun afterEvaluate(project: Project, apolloExtension: DefaultApolloExtension) {
      val checkVersionsTask = registerCheckVersionsTask(project)
      registerCompilationUnits(project, apolloExtension, checkVersionsTask)

      registerDownloadSchemaTasks(project, apolloExtension)
      project.tasks.register(ModelNames.convertApolloSchema(), ApolloConvertSchemaTask::class.java) { task ->
        task.group = TASK_GROUP
      }
    }

    data class Dep(val name: String, val version: String?)

    fun getDeps(configurations: List<Configuration>): List<Dep> {
      return configurations.flatMap { configuration ->
        configuration.incoming.dependencies
            .filter {
              it.group == "com.apollographql.apollo"
            }.map { dependency ->
              Dep(dependency.name, dependency.version)
            }
      }
    }

    fun registerCheckVersionsTask(project: Project): TaskProvider<Task> {
      return project.tasks.register(ModelNames.checkApolloVersions()) {
        val outputFile = BuildDirLayout.versionCheck(project)

        it.outputs.file(outputFile)

        it.inputs.property("versions") {
          val allConfigurations = project.rootProject.buildscript.configurations +
                  project.buildscript.configurations +
                  project.configurations

          /**
           * This includes all the configurations in the dependency resolution so it
           * "freezes" them and adding dependencies after [getDeps] fails with:
           *
           * Cannot change dependencies of dependency configuration '$configuration'
           * after it has been included in dependency resolution.
           *
           * Since this is lazy, it should hopefully be called after all other plugins
           * have had a chance to change the dependencies
           */
          getDeps(allConfigurations.toList())
                  .mapNotNull { it.version }
                  .distinct()
                  .sorted()
        }

        it.doLast(object: Action<Task> {
          override fun execute(t: Task) {
            val allVersions = it.inputs.properties["versions"] as List<String>

            check(allVersions.size <= 1) {
              "ApolloGraphQL: All apollo versions should be the same. Found:\n$allVersions"
            }

            val version = allVersions.firstOrNull()
            outputFile.get().asFile.parentFile.mkdirs()
            outputFile.get().asFile.writeText("All versions are consistent: $version")

          }
        })
      }

    }
  }

  override fun apply(project: Project) {
    require(GradleVersion.current().compareTo(GradleVersion.version(MIN_GRADLE_VERSION)) >= 0) {
      "apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater"
    }

    val apolloExtension = project.extensions.create(ApolloExtension::class.java, "apollo", DefaultApolloExtension::class.java, project) as DefaultApolloExtension

    project.configurations.create(ModelNames.apolloConfiguration()) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = false

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
      }
    }

    // the extension block has not been evaluated yet, register a callback once the project has been evaluated
    project.afterEvaluate {
      afterEvaluate(it, apolloExtension)
    }
  }
}
