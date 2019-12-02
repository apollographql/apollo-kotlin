package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.ApolloSourceSetExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import java.net.URLDecoder

open class ApolloPlugin : Plugin<Project> {
  companion object {
    const val TASK_GROUP = "apollo"

    fun useService(project: Project, schemaFilePath: String?, outputPackageName: String? = null, exclude: String? = null): String {

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

    private fun deprecationChecks(apolloExtension: DefaultApolloExtension, apolloSourceSetExtension: ApolloSourceSetExtension) {
      if (apolloSourceSetExtension.schemaFile.isPresent || apolloSourceSetExtension.exclude.get().isNotEmpty()) {
        throw IllegalArgumentException("""
        apollo.sourceSet is not supported anymore.
        
      """.trimIndent() + useService(apolloExtension.project, apolloSourceSetExtension.schemaFile.orNull,
            null, "[${apolloSourceSetExtension.exclude.get().joinToString(",")}]"))
      }

      if (apolloExtension.schemaFilePath.isPresent) {
        throw IllegalArgumentException("""
        apollo.schemaFilePath is not supported anymore as it doesn't work for multiple services.
        
      """.trimIndent() + useService(apolloExtension.project, apolloExtension.schemaFilePath.get(), apolloExtension.outputPackageName.orNull))
      }

      if (apolloExtension.outputPackageName.isPresent) {
        throw IllegalArgumentException("""
        apollo.outputPackageName is not supported anymore as it doesn't work for multiple services and also flattens the packages.
        
      """.trimIndent() + useService(apolloExtension.project, apolloExtension.schemaFilePath.orNull, apolloExtension.outputPackageName.get()))
      }
    }


    private fun registerCodegenTasks(project: Project, apolloExtension: DefaultApolloExtension) {
      val androidExtension = project.extensions.findByName("android")

      val apolloVariants = if (androidExtension == null) {
        JvmTaskConfigurator.getVariants(project)
      } else {
        AndroidTaskConfigurator.getVariants(project, androidExtension)
      }

      val rootProvider = registerRootTask(project)

      apolloVariants.all { apolloVariant ->
        val variantProvider = registerVariantTask(project, apolloVariant.name)

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

          if (androidExtension == null) {
            JvmTaskConfigurator.registerGeneratedDirectory(project, compilationUnit, codegenProvider)
          } else {
            AndroidTaskConfigurator.registerGeneratedDirectory(project, androidExtension, compilationUnit, codegenProvider)
          }

          compilationUnit.outputDir.set(codegenProvider.flatMap { it.outputDir })
          compilationUnit.transformedQueriesDir.set(codegenProvider.flatMap { it.transformedQueriesOutputDir })

          apolloExtension.compilationUnits.add(compilationUnit)
        }

        rootProvider.configure {
          it.dependsOn(variantProvider)
        }

      }
    }

    private fun registerVariantTask(project: Project, variantName: String): TaskProvider<Task> {
      // for backward compatibility
      val oldName = "generate${variantName.capitalize()}ApolloClasses"
      project.tasks.register(oldName) {
        it.group = TASK_GROUP
        it.doLast {
          throw IllegalArgumentException("$oldName is deprecated. Please use generateApolloSources instead.")
        }
      }

      return project.tasks.register("generate${variantName.capitalize()}ApolloSources") {
        it.group = TASK_GROUP
      }
    }

    private fun registerRootTask(project: Project): TaskProvider<*> {
      // for backward compatibility
      project.tasks.register("generateApolloClasses") {
        it.group = TASK_GROUP
        it.doLast {
          throw IllegalArgumentException("generateApolloClasses is deprecated. Please use generateApolloSources instead.")
        }
      }

      return project.tasks.register("generateApolloSources") {
        it.group = TASK_GROUP
      }
    }

    fun registerCodeGenTask(project: Project, compilationUnit: DefaultCompilationUnit): TaskProvider<ApolloGenerateSourcesTask> {
      val taskName = "generate${compilationUnit.name.capitalize()}ApolloSources"

      return project.tasks.register(taskName, ApolloGenerateSourcesTask::class.java) {
        it.group = TASK_GROUP
        it.description = "Generate Apollo models for ${compilationUnit.name.capitalize()} GraphQL queries"

        val compilerParams = compilationUnit
            .withFallback(project.objects, compilationUnit.service)
            .withFallback(project.objects, compilationUnit.apolloExtension)

        compilationUnit.setSourcesIfNeeded(compilerParams.graphqlSourceDirectorySet, compilerParams.schemaFile)

        it.graphqlFiles.setFrom(compilerParams.graphqlSourceDirectorySet)
        // I'm not sure if gradle is sensitive to the order of the rootFolders. Sort them just in case.
        it.rootFolders.set(project.provider { compilerParams.graphqlSourceDirectorySet.srcDirs.map { it.absolutePath }.sorted() })
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
        it.transformedQueriesOutputDir.apply {
          if (compilerParams.generateTransformedQueries.getOrElse(false)) {
            set(project.layout.buildDirectory.map {
              it.dir("generated/transformedQueries/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}")
            })
          }
          disallowChanges()
        }
        it.generateAsInternal.set(compilerParams.generateAsInternal)
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

    private fun afterEvaluate(project: Project, apolloExtension: DefaultApolloExtension, apolloSourceSetExtension: ApolloSourceSetExtension) {

      deprecationChecks(apolloExtension, apolloSourceSetExtension)

      registerCodegenTasks(project, apolloExtension)

      registerDownloadSchemaTasks(project, apolloExtension)
    }
  }

  override fun apply(project: Project) {
    require (GradleVersion.current().compareTo(GradleVersion.version("5.6")) >= 0) {
      "apollo-android requires Gradle version 5.6 or greater"
    }

    val apolloExtension = project.extensions.create(ApolloExtension::class.java, "apollo", DefaultApolloExtension::class.java, project) as DefaultApolloExtension
    // for backward compatibility
    val apolloSourceSetExtension = (apolloExtension as ExtensionAware).extensions.create("sourceSet", ApolloSourceSetExtension::class.java, project.objects)

    // the extension block has not been evaluated yet, register a callback once the project has been evaluated
    project.afterEvaluate {
      afterEvaluate(it, apolloExtension, apolloSourceSetExtension)
    }
  }
}
