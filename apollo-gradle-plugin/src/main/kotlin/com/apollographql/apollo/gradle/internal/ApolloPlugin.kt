package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.OperationOutputGenerator
import com.apollographql.apollo.gradle.api.ApolloExtension
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.GradleVersion
import java.net.URLDecoder

open class ApolloPlugin : Plugin<Project> {
  internal interface Variant: Named {
    companion object {
      val APOLLO_VARIANT_ATTRIBUTE = Attribute.of("com.apollographql.variant", Variant::class.java)
    }
  }
  internal interface Service: Named {
    companion object {
      val APOLLO_SERVICE_ATTRIBUTE = Attribute.of("com.apollographql.service", Service::class.java)
    }
  }

  internal companion object {
    const val TASK_GROUP = "apollo"
    const val MIN_GRADLE_VERSION = "6.0"

    const val CONFIGURATION_CONSUMER = "apollo"
    const val USAGE_APOLLO_METADATA = "apollo-metadata"

    val Project.isKotlinMultiplatform get() = pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")

    private fun registerCompilationUnits(project: Project, apolloExtension: DefaultApolloExtension, checkVersionsTask: TaskProvider<Task>) {
      val androidExtension = project.extensions.findByName("android")

      val apolloConfiguration = project.configurations.getByName(ModelNames.consumerConfiguration())

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

            it.attributes {
              it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
              it.attribute(Variant.APOLLO_VARIANT_ATTRIBUTE, project.objects.named(Variant::class.java, compilationUnit.variantName))
              it.attribute(Service.APOLLO_SERVICE_ATTRIBUTE, project.objects.named(Service::class.java, compilationUnit.serviceName))
            }
          }

          val consumerConfiguration = project.configurations.create(ModelNames.consumerConfiguration(compilationUnit)) {
            it.isCanBeResolved = true
            it.isCanBeConsumed = false

            it.extendsFrom(apolloConfiguration)

            it.attributes {
              it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_APOLLO_METADATA))
              it.attribute(Variant.APOLLO_VARIANT_ATTRIBUTE, project.objects.named(Variant::class.java, compilationUnit.variantName))
              it.attribute(Service.APOLLO_SERVICE_ATTRIBUTE, project.objects.named(Service::class.java, compilationUnit.serviceName))
            }
          }

          val codegenProvider = registerCodeGenTask(project, compilationUnit, consumerConfiguration)

          val zipMetadataTaskProvider = project.tasks.register(ModelNames.zipMetadata(compilationUnit), Zip::class.java) {
            it.group = TASK_GROUP
            it.description = "Generate apolloMetadata.zip for multi-module builds"

            val file = BuildDirLayout.metadataZip(project, compilationUnit).get().asFile
            it.destinationDirectory.set(file.parentFile)
            it.archiveFileName.set(file.name)
            it.from(codegenProvider.map { it.metadataOutputDir }) {
              it.into("metadata")
            }
            it.dependsOn(rootProvider)
          }

          project.artifacts {
            it.add(producerConfigurationName, zipMetadataTaskProvider)
          }

          codegenProvider.configure {
            it.dependsOn(checkVersionsTask)
            it.dependsOn(consumerConfiguration)
          }

          variantProvider.configure {
            it.dependsOn(codegenProvider)
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
        }

        rootProvider.configure {
          it.dependsOn(variantProvider)
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
        task.metadataOutputDir.set(BuildDirLayout.metadata(project, compilationUnit))

        task.metadataConfiguration = consumerConfiguration

        task.rootPackageName.set(compilerParams.rootPackageName)
        task.generateAsInternal.set(compilerParams.generateAsInternal)
        task.kotlinMultiPlatformProject.set(project.isKotlinMultiplatform)
        task.sealedClassesForEnumsMatching.set(compilerParams.sealedClassesForEnumsMatching)
        task.generateApolloMetadata.set(compilerParams.generateApolloMetadata)
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
    }

    data class Dep(val name: String, val version: String?)

    fun getDeps(configurations: ConfigurationContainer): List<Dep> {
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
  }

  override fun apply(project: Project) {
    require(GradleVersion.current().compareTo(GradleVersion.version(MIN_GRADLE_VERSION)) >= 0) {
      "apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater"
    }

    val apolloExtension = project.extensions.create(ApolloExtension::class.java, "apollo", DefaultApolloExtension::class.java, project) as DefaultApolloExtension

    project.configurations.create(ModelNames.consumerConfiguration()) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true

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
