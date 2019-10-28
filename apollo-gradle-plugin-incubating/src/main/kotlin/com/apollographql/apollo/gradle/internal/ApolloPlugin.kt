package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.child
import com.apollographql.apollo.gradle.api.ApolloSourceSetExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider

open class ApolloPlugin : Plugin<Project> {
  companion object {
    const val TASK_GROUP = "apollo"

    fun useService(schemaFilePath: String?, outputPackageName: String? = null, exclude: String? = null): String {
      var ret = """
      |Please use a service instead:
      |apollo {
      |  service("github") {
      """.trimMargin()

      if (schemaFilePath != null) {
        ret += "\n    schemaFilePath = \"$schemaFilePath\""
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
        
      """.trimIndent() + useService(apolloSourceSetExtension.schemaFile.getOrElse("null"),
            null, "[${apolloSourceSetExtension.exclude.get().joinToString(",")}]"))
      }

      if (apolloExtension.schemaFilePath.isPresent) {
        throw IllegalArgumentException("""
        apollo.schemaFilePath is not supported anymore as it doesn't work for multiple services.
        
      """.trimIndent() + ApolloPlugin.useService(apolloExtension.schemaFilePath.get(), apolloExtension.outputPackageName.getOrElse("null")))
      }

      if (apolloExtension.outputPackageName.isPresent) {
        throw IllegalArgumentException("""
        apollo.outputPackageName is not supported anymore as it doesn't work for multiple services and also flattens the packages.
        
      """.trimIndent() + ApolloPlugin.useService(apolloExtension.schemaFilePath.getOrElse("null"), apolloExtension.outputPackageName.get()))
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
          DefaultCompilationUnit.fromFiles(project, apolloExtension, apolloVariant)
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
        it.description = "Generate Android classes for ${compilationUnit.name.capitalize()} GraphQL queries"

        val sources = compilationUnit.sources()

        it.graphqlFiles.setFrom(sources.graphqlFiles)
        it.rootFolders.set(sources.rootFolders.map { it.absolutePath })
        it.schemaFile.set(sources.schemaFile)
        it.rootPackageName.set(sources.rootPackageName)

        it.nullableValueType.set(compilationUnit.compilerParams.nullableValueType)
        it.useSemanticNaming.set(compilationUnit.compilerParams.useSemanticNaming)
        it.generateModelBuilder.set(compilationUnit.compilerParams.generateModelBuilder)
        it.useJavaBeansSemanticNaming.set(compilationUnit.compilerParams.useJavaBeansSemanticNaming)
        it.suppressRawTypesWarning.set(compilationUnit.compilerParams.suppressRawTypesWarning)
        it.generateKotlinModels.set(compilationUnit.compilerParams.generateKotlinModels)
        it.generateVisitorForPolymorphicDatatypes.set(compilationUnit.compilerParams.generateVisitorForPolymorphicDatatypes)
        it.customTypeMapping.set(compilationUnit.compilerParams.customTypeMapping)
        it.outputDir.apply {
          set(project.layout.buildDirectory.map {
            it.dir("generated/source/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}")
          })
          disallowChanges()
        }
        it.transformedQueriesOutputDir.apply {
          if (compilationUnit.compilerParams.generateTransformedQueries.getOrElse(false)) {
            set(project.layout.buildDirectory.map {
              it.dir("generated/transformedQueries/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}")
            })
          }
          disallowChanges()
        }
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

            /**
             * We cannot know in advance if the backend schema changed so don't cache or mark this task up-to-date
             * This code actually redundant because the task has no output but adding it make it explicit.
             */
            task.outputs.upToDateWhen { false }
            task.outputs.cacheIf { false }
          }
        }
      }
    }

    private fun afterEvaluate(project: Project, apolloExtension: DefaultApolloExtension, apolloSourceSetExtension: ApolloSourceSetExtension) {

      deprecationChecks(apolloExtension, apolloSourceSetExtension)

      registerCodegenTasks(project, apolloExtension)

      registerDownloadSchemaTasks(project, apolloExtension)
    }
  }

  override fun apply(project: Project) {
    val apolloExtension = project.extensions.create("apollo", DefaultApolloExtension::class.java, project)
    // for backward compatibility
    val apolloSourceSetExtension = (apolloExtension as ExtensionAware).extensions.create("sourceSet", ApolloSourceSetExtension::class.java, project.objects)

    // the extension block has not been evaluated yet, register a callback once the project has been evaluated
    project.afterEvaluate {
      afterEvaluate(it, apolloExtension, apolloSourceSetExtension)
    }
  }
}