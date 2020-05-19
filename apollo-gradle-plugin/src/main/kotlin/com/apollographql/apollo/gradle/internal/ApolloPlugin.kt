package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import java.net.URLDecoder

open class ApolloPlugin : Plugin<Project> {
  companion object {
    const val TASK_GROUP = "apollo"
    const val MIN_GRADLE_VERSION = "6.0"

    val Project.isKotlinMultiplatform get() = pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")

    private fun useService(project: Project, schemaFilePath: String?, outputPackageName: String? = null, exclude: String? = null): String {

      var ret = """
      |Please use a service instead:
      |apollo {
      |  service("github") {
      """.trimMargin()

      if (schemaFilePath != null) {
        val match = Regex("src/.*/graphql/(.*)").matchEntire(schemaFilePath)
        val schemaPath = if (match != null) {
          match.groupValues[1]
        } else {
          project.file(schemaFilePath).absolutePath
        }
        ret += "\n    schemaPath = \"$schemaPath\""
      }
      if (outputPackageName != null) {
        ret += "\n    rootPackageName = \"$outputPackageName\""
      }
      if (exclude != null) {
        ret += "\n    exclude = $exclude"
      }
      ret += """
      |
      |  }
      |}
    """.trimMargin()
      return ret
    }

    private fun registerCodeGenTasks(project: Project, apolloExtension: DefaultApolloExtension) {
      val androidExtension = project.extensions.findByName("android")

      val apolloVariants = when {
        project.isKotlinMultiplatform -> KotlinMultiplatformTaskConfigurator.getVariants(project)
        androidExtension != null -> AndroidTaskConfigurator.getVariants(project, androidExtension)
        else -> JvmTaskConfigurator.getVariants(project)
      }

      val rootProvider = project.tasks.register("generateApolloSources") {
        it.group = TASK_GROUP
      }

      apolloVariants.all { apolloVariant ->
        val variantProvider = project.tasks.register("generate${apolloVariant.name.capitalize()}ApolloSources") {
          it.group = TASK_GROUP
        }

        val compilationUnits = if (apolloExtension.services.isEmpty()) {
          listOf(DefaultCompilationUnit.fromFiles(project, apolloExtension, apolloVariant))
        } else {
          apolloExtension.services.map {
            DefaultCompilationUnit.fromService(project, apolloExtension, apolloVariant, it)
          }
        }

        compilationUnits.forEach { compilationUnit ->
          val codegenProvider = registerCodeGenTask(project, compilationUnit)
          variantProvider.configure {
            it.dependsOn(codegenProvider)
          }

          compilationUnit.outputDir.set(codegenProvider.flatMap { it.outputDir })
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

    private fun registerCodeGenTask(project: Project, compilationUnit: DefaultCompilationUnit): TaskProvider<ApolloGenerateSourcesTask> {
      val taskName = "generate${compilationUnit.name.capitalize()}ApolloSources"

      return project.tasks.register(taskName, ApolloGenerateSourcesTask::class.java) {
        it.group = TASK_GROUP
        it.description = "Generate Apollo models for ${compilationUnit.name.capitalize()} GraphQL queries"

        val compilerParams = compilationUnit
            .withFallback(project.objects, compilationUnit.service)
            .withFallback(project.objects, compilationUnit.apolloExtension)

        val graphqlSourceDirectorySet = if (compilationUnit.apolloVariant.isTest) {
          // For tests, reusing sourceDirectorySet from the Service or Extension will
          // generate duplicate classes so we just skip them
          compilationUnit.graphqlSourceDirectorySet
        } else {
          compilerParams.graphqlSourceDirectorySet
        }
        compilationUnit.setSourcesIfNeeded(graphqlSourceDirectorySet, compilerParams.schemaFile)

        it.graphqlFiles.setFrom(graphqlSourceDirectorySet)
        // I'm not sure if gradle is sensitive to the order of the rootFolders. Sort them just in case.
        it.rootFolders.set(project.provider { graphqlSourceDirectorySet.srcDirs.map { it.absolutePath }.sorted() })
        it.schemaFile.set(compilerParams.schemaFile)

        it.nullableValueType.set(compilerParams.nullableValueType)
        it.useSemanticNaming.set(compilerParams.useSemanticNaming)
        it.generateModelBuilder.set(compilerParams.generateModelBuilder)
        it.useJavaBeansSemanticNaming.set(compilerParams.useJavaBeansSemanticNaming)
        it.suppressRawTypesWarning.set(compilerParams.suppressRawTypesWarning)
        it.generateKotlinModels.set(compilationUnit.generateKotlinModels())
        it.generateVisitorForPolymorphicDatatypes.set(compilerParams.generateVisitorForPolymorphicDatatypes)
        it.customTypeMapping.set(compilerParams.customTypeMapping)
        it.rootPackageName.set(compilerParams.rootPackageName)
        it.outputDir.apply {
          set(project.layout.buildDirectory.map {
            it.dir("generated/source/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}")
          })
          disallowChanges()
        }
        it.operationOutputFile.apply {
          if (compilerParams.generateOperationOutput.getOrElse(false)) {
            set(project.layout.buildDirectory.file("generated/operationOutput/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}/OperationOutput.json"))
          }
          disallowChanges()
        }

        it.generateAsInternal.set(compilerParams.generateAsInternal)
        it.operationIdGenerator.set(compilerParams.operationIdGenerator)
        it.kotlinMultiPlatformProject.set(project.isKotlinMultiplatform)
        it.sealedClassesForEnumsMatching.set(compilerParams.sealedClassesForEnumsMatching)
        Unit
      }
    }

    private fun registerDownloadSchemaTasks(project: Project, apolloExtension: DefaultApolloExtension) {
      apolloExtension.services.forEach { service ->
        val introspection = service.introspection
        if (introspection != null) {
          project.tasks.register("download${service.name.capitalize()}ApolloSchema", ApolloDownloadSchemaTask::class.java) { task ->

            val sourceSetName = introspection.sourceSetName.orElse("main")
            task.group = TASK_GROUP
            task.schemaFilePath.set(service.schemaPath.map { "src/${sourceSetName.get()}/graphql/$it" })
            task.endpointUrl.set(introspection.endpointUrl)
            task.queryParameters.set(introspection.queryParameters)
            task.headers.set(introspection.headers)
          }
        }
      }

      project.tasks.register("downloadApolloSchema", ApolloDownloadSchemaTask::class.java) { task ->
        task.group = TASK_GROUP

        task.schemaFilePath.set(project.provider {
          val schema = project.findProperty("com.apollographql.apollo.schema") as? String
          require(schema != null) {
            "downloadApolloSchema requires setting -Pcom.apollographql.apollo.schema=/path/to/your/schema.json"
          }
          schema
        })

        task.endpointUrl.set(project.provider {
          val endpoint = project.findProperty("com.apollographql.apollo.endpoint") as? String
          require(endpoint != null) {
            "downloadApolloSchema requires setting -Pcom.apollographql.apollo.endpoint=https://your.graphql.endpoint"
          }
          endpoint
        })

        task.queryParameters.set(project.provider {
          (project.findProperty("com.apollographql.apollo.query_params") as? String)
              ?.let {
                toMap(it)
              } ?: emptyMap()
        })
        task.headers.set(project.provider {
          (project.findProperty("com.apollographql.apollo.headers") as? String)
              ?.let {
                toMap(it)
              } ?: emptyMap()
        })
      }
    }

    private fun toMap(s: String): Map<String, String> {
      return s.split("&")
          .map {
            val keyValue = it.split("=")
            val key = URLDecoder.decode(keyValue[0], "UTF-8")
            val value = URLDecoder.decode(keyValue[1], "UTF-8")

            key to value
          }.toMap()
    }

    private fun afterEvaluate(project: Project, apolloExtension: DefaultApolloExtension) {
      registerCodeGenTasks(project, apolloExtension)

      registerDownloadSchemaTasks(project, apolloExtension)

      checkVersions(project)
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

    fun checkVersions(project: Project) {
      val allDeps = getDeps(project.rootProject.buildscript.configurations) +
          getDeps(project.buildscript.configurations) +
          getDeps(project.configurations)

      check(allDeps.mapNotNull { it.version }.distinct().size <= 1) {
        val found = allDeps.map { "${it.name}:${it.version}" }.distinct().joinToString("\n")
        "All apollo versions should be the same. Found:\n$found"
      }
    }
  }

  override fun apply(project: Project) {
    require(GradleVersion.current().compareTo(GradleVersion.version(MIN_GRADLE_VERSION)) >= 0) {
      "apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater"
    }

    val apolloExtension = project.extensions.create(ApolloExtension::class.java, "apollo", DefaultApolloExtension::class.java, project) as DefaultApolloExtension

    // the extension block has not been evaluated yet, register a callback once the project has been evaluated
    project.afterEvaluate {
      afterEvaluate(it, apolloExtension)
    }
  }
}
