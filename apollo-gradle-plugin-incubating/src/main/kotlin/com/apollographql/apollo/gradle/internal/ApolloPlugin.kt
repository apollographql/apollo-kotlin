package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloExtension
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

    private fun deprecationChecks(apolloSourceSetExtension: ApolloSourceSetExtension) {
      if (apolloSourceSetExtension.schemaFile != null || apolloSourceSetExtension.exclude != null) {
        throw IllegalArgumentException("""
        apollo.sourceSet is not supported anymore.
        
      """.trimIndent() + useService(apolloSourceSetExtension.schemaFile, null, "[${apolloSourceSetExtension.exclude?.joinToString(",")}]"))
      }
    }



    private fun registerCodegenTasks(project: Project, apolloExtension: ApolloExtension) {
      val androidExtension = project.extensions.findByName("android")

      val apolloVariants = if (androidExtension == null) {
        JvmTaskConfigurator.getVariants(project)
      } else {
        AndroidTaskConfigurator.getVariants(project, androidExtension)
      }

      val rootProvider = registerRootTask(project)

      apolloVariants.all {apolloVariant ->
        val variantProvider = registerVariantTask(project, apolloVariant.name)

        val compilationUnits = if (apolloExtension.services.isEmpty()) {
          DefaultCompilationUnit.default(project, apolloExtension, apolloVariant)
        } else {
            apolloExtension.services.map {
              DefaultCompilationUnit.from(project, apolloExtension, apolloVariant, it)
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

          compilationUnit.outputDir = codegenProvider.flatMap { it.outputDir }
          compilationUnit.transformedQueriesDir = codegenProvider.flatMap { it.transformedQueriesOutputDir }

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
      val variantName = compilationUnit.variantName
      val taskName = "generate${variantName.capitalize()}${compilationUnit.serviceName.capitalize()}ApolloSources"

      return project.tasks.register(taskName, ApolloGenerateSourcesTask::class.java) {
        it.source(compilationUnit.files)
        if (compilationUnit.schemaFile != null) {
          it.source(compilationUnit.schemaFile)
        }

        it.graphqlFiles = compilationUnit.files
        it.schemaFile = compilationUnit.schemaFile
        it.schemaPackageName = compilationUnit.schemaPackageName
        it.rootPackageName = compilationUnit.rootPackageName
        it.group = TASK_GROUP
        it.description = "Generate Android classes for ${variantName.capitalize()}${compilationUnit.serviceName} GraphQL queries"
        it.nullableValueType = compilationUnit.compilerParams.nullableValueType
        it.useSemanticNaming = compilationUnit.compilerParams.useSemanticNaming
        it.generateModelBuilder = compilationUnit.compilerParams.generateModelBuilder
        it.useJavaBeansSemanticNaming = compilationUnit.compilerParams.useJavaBeansSemanticNaming
        it.suppressRawTypesWarning = compilationUnit.compilerParams.suppressRawTypesWarning
        it.generateKotlinModels = compilationUnit.compilerParams.generateKotlinModels
        it.generateVisitorForPolymorphicDatatypes = compilationUnit.compilerParams.generateVisitorForPolymorphicDatatypes
        it.customTypeMapping = compilationUnit.compilerParams.customTypeMapping
        it.outputDir.apply {
          set(compilationUnit.outputDirectory)
          disallowChanges()
        }
        it.transformedQueriesOutputDir.apply {
          if (compilationUnit.compilerParams.generateTransformedQueries) {
            set(compilationUnit.transformedQueriesDirectory)
          }
          disallowChanges()
        }
      }
    }

    private fun registerDownloadSchemaTasks(project: Project, apolloExtension: ApolloExtension) {
      apolloExtension.services.forEach { service ->
        val introspection = service.introspection
        if (introspection != null) {
          project.tasks.register("download${service.name.capitalize()}ApolloSchema", ApolloDownloadSchemaTask::class.java) { task ->
            task.group = TASK_GROUP
            task.schemaFilePath = service.schemaFilePath
            task.endpointUrl = introspection.endpointUrl!!
            task.queryParameters = introspection.queryParameters
            task.headers = introspection.headers
          }
        }
      }
    }

    private fun afterEvaluate(project: Project, apolloExtension: ApolloExtension, apolloSourceSetExtension: ApolloSourceSetExtension) {

      deprecationChecks(apolloSourceSetExtension)

      registerCodegenTasks(project, apolloExtension)

      registerDownloadSchemaTasks(project, apolloExtension)
    }
  }

  override fun apply(project: Project) {
    val apolloExtension = project.extensions.create("apollo", ApolloExtension::class.java, project)
    // for backward compatibility
    val apolloSourceSetExtension = (apolloExtension as ExtensionAware).extensions.create("sourceSet", ApolloSourceSetExtension::class.java)

    // the extension block has not been evaluated yet, register a callback once the project has been evaluated
    project.afterEvaluate {
      afterEvaluate(it, apolloExtension, apolloSourceSetExtension)
    }
  }
}