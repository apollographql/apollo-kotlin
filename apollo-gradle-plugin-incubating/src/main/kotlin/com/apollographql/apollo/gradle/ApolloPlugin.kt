package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.child
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider

open class ApolloPlugin : Plugin<Project> {
  companion object {
    const val TASK_GROUP = "apollo"
  }

  private lateinit var apolloExtension: ApolloExtension
  private lateinit var apolloSourceSetExtension: ApolloSourceSetExtension
  private lateinit var generateClassesTask: TaskProvider<Task>

  override fun apply(project: Project) {
    apolloExtension = project.extensions.create("apollo", ApolloExtension::class.java)

    // for backward compatibility
    apolloSourceSetExtension = (apolloExtension as ExtensionAware).extensions.create("sourceSet", ApolloSourceSetExtension::class.java)

    generateClassesTask = project.tasks.register("generateApolloClasses") {
      it.group = TASK_GROUP
    }

    // the extension block has not been evaluated yet, register a callback once the project has been evaluated
    project.afterEvaluate {
      afterEvaluate(it)
    }
  }

  private fun useService(schemaFilePath: String?, outputPackageName: String? = null, exclude: String? = null): String {
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

  private fun afterEvaluate(project: Project) {

    val androidExtension = project.extensions.findByName("android")

    if (apolloExtension.generateKotlinModels && apolloExtension.generateModelBuilder) {
      throw IllegalArgumentException("""
        Using `generateModelBuilder = true` does not make sense with `generateKotlinModels = true`. You can use .copy() as models are data classes.
      """.trimIndent())
    }

    if (apolloExtension.generateKotlinModels && apolloExtension.useJavaBeansSemanticNaming) {
      throw IllegalArgumentException("""
        Using `useJavaBeansSemanticNaming = true` does not make sense with `generateKotlinModels = true`
      """.trimIndent())
    }

    if (apolloExtension.generateKotlinModels && apolloExtension.nullableValueType != null) {
      throw IllegalArgumentException("""
        Using `nullableValueType = true` does not make sense with `generateKotlinModels = true`
      """.trimIndent())
    }

    if (apolloExtension.schemaFilePath != null) {
      throw IllegalArgumentException("""
        apollo.schemaFilePath is not supported anymore as it doesn't work for multiple services.
        
      """.trimIndent() + useService(apolloExtension.schemaFilePath, apolloExtension.outputPackageName))
    }

    if (apolloExtension.outputPackageName != null) {
      throw IllegalArgumentException("""
        apollo.outputPackageName is not supported anymore as it doesn't work for multiple services and also flattens the packages.
        
      """.trimIndent() + useService(apolloExtension.schemaFilePath, apolloExtension.outputPackageName))
    }

    if (apolloSourceSetExtension.schemaFile != null || apolloSourceSetExtension.exclude != null) {
      throw IllegalArgumentException("""
        apollo.sourceSet is not supported anymore.
        
      """.trimIndent() + useService(apolloSourceSetExtension.schemaFile, null, "[${apolloSourceSetExtension.exclude?.joinToString(",")}]"))
    }

    if (androidExtension == null) {
      val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
      val sourceSets = javaPlugin.sourceSets

      // TODO: should we add tasks for the test sourceSet ?
      val name = "main"
      val apolloVariant = ApolloVariant(
          name = name,
          sourceSetNames = listOf(name)
      )

      registerVariantTask(project, apolloVariant) { serviceVariantTask ->
        val sourceDirectorySet = if (!apolloExtension.generateKotlinModels) {
          sourceSets.getByName(name).getJava()
        } else {
          sourceSets.getByName(name).kotlin!!
        }
        val compileTaskName = if (!apolloExtension.generateKotlinModels) {
          "compileJava"
        } else {
          "compileKotlin"
        }
        sourceDirectorySet.srcDir(serviceVariantTask.get().outputDir)
        project.tasks.named(compileTaskName).configure { it.dependsOn(serviceVariantTask) }
      }
    } else {
      AndroidTaskConfigurator.configure(apolloExtension, androidExtension, project, this::registerVariantTask)
    }

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

  private fun registerVariantTask(project: Project,
                                  apolloVariant: ApolloVariant,
                                  addOutputDirToSourceSetDir: (TaskProvider<ApolloCodegenTask>) -> Unit) {

    val generateVariantClassesTask = project.tasks.register("generate${apolloVariant.name.capitalize()}ApolloClasses") {
      it.group = TASK_GROUP
    }

    var serviceVariants = apolloExtension.services.map {
      ServiceVariant.from(project, apolloVariant.sourceSetNames, it)
    }
    if (serviceVariants.isEmpty()) {
      serviceVariants = ServiceVariant.default(project, apolloVariant.sourceSetNames)
    }
    serviceVariants.forEach { serviceVariant ->
      val serviceVariantTask = registerCodegenTasks(project, apolloExtension, apolloVariant.name, serviceVariant)
      generateVariantClassesTask.configure {
        it.dependsOn(serviceVariantTask)
      }

      addOutputDirToSourceSetDir(serviceVariantTask)
    }

    generateClassesTask.configure {
      it.dependsOn(generateVariantClassesTask)
    }
  }

  fun registerCodegenTasks(project: Project, extension: ApolloExtension, variantName: String, serviceVariant: ServiceVariant): TaskProvider<ApolloCodegenTask> {
    val taskName = "generate${variantName.capitalize()}${serviceVariant.name.capitalize()}ApolloClasses"

    return project.tasks.register(taskName, ApolloCodegenTask::class.java) {
      val outputFolder = project.buildDir.child("generated", "apollo", "classes", variantName, serviceVariant.name)

      val transformedQueriesOutputDir = if (extension.generateTransformedQueries) {
        project.buildDir.child("generated", "apollo", "transformedQueries", variantName, serviceVariant.name)
      } else {
        null
      }

      it.source(serviceVariant.files)
      if (serviceVariant.schemaFile != null) {
        it.source(serviceVariant.files)
      }

      it.graphqlFiles = serviceVariant.files
      it.schemaFile = serviceVariant.schemaFile
      it.schemaPackageName = serviceVariant.schemaPackageName
      it.rootPackageName = serviceVariant.rootPackageName
      it.group = TASK_GROUP
      it.description = "Generate Android classes for ${variantName.capitalize()}${serviceVariant.name} GraphQL queries"
      it.outputDir = outputFolder
      it.nullableValueType = extension.nullableValueType ?: NullableValueType.ANNOTATED.value
      it.useSemanticNaming = extension.useSemanticNaming
      it.generateModelBuilder = extension.generateModelBuilder
      it.useJavaBeansSemanticNaming = extension.useJavaBeansSemanticNaming
      it.suppressRawTypesWarning = extension.suppressRawTypesWarning
      it.generateKotlinModels = extension.generateKotlinModels
      it.generateVisitorForPolymorphicDatatypes = extension.generateVisitorForPolymorphicDatatypes
      it.customTypeMapping = extension.customTypeMapping
      it.transformedQueriesOutputDir = transformedQueriesOutputDir
    }
  }
}