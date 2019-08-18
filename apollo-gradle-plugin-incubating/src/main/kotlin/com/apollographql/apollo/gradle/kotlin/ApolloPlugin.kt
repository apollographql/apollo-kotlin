package com.apollographql.apollo.gradle.kotlin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.apollographql.apollo.compiler.GraphQLCompiler
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.file.FileResolver
import java.io.File
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import javax.inject.Inject
import kotlin.IllegalArgumentException


class ApolloPlugin : Plugin<Project> {
  companion object {
    const val TASK_GROUP = "apollo"
  }

  lateinit var apolloExtension: ApolloExtension

  override fun apply(project: Project) {
    apolloExtension = project.extensions.create("apollo", ApolloExtension::class.java)

    // the extension block has not been evaluated yet, register a callback once the project has been evaluated
    project.afterEvaluate {
      afterEvaluate(it)
    }
  }

  private fun afterEvaluate(project: Project) {
    val generateClassesTask = project.tasks.register("generateApolloClasses") {
      it.group = TASK_GROUP
    }

    val androidExtension = project.extensions.findByName("android")

    apolloExtension.validate()

    when {
      androidExtension is LibraryExtension -> {
        androidExtension.libraryVariants.all(Action { variant ->
          registerAndroid(project, apolloExtension, variant, generateClassesTask)
        })
        androidExtension.testVariants.all(Action { variant ->
          registerAndroid(project, apolloExtension, variant, generateClassesTask)
        })
      }
      androidExtension is AppExtension -> {
        androidExtension.applicationVariants.all(Action { variant ->
          registerAndroid(project, apolloExtension, variant, generateClassesTask)
        })
      }
      androidExtension == null -> {
        val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
        val sourceSets = javaPlugin.sourceSets

        sourceSets.forEach {
          val apolloVariant = ApolloVariant(
              name = it.name,
              sourceSetNames = listOf(it.name)
          )

          val task = registerVariantTask(project, apolloExtension, apolloVariant) { task ->
            if (!apolloExtension.generateKotlinModels) {
              sourceSets.getByName(it.name).getJava().srcDir(task.get().outputDir)
              // TODO: make something for compileTestJava ?
              project.tasks.named("compileJava").configure { it.dependsOn(task) }
            } else {
              // https://github.com/gradle/gradle/issues/3191
              //   ¯\_(ツ)_/¯
              val kotlinSourceDirectorySet = (sourceSets.getByName(it.name) as HasConvention)
                  .convention
                  .getPlugin(KotlinSourceSet::class.java)
                  .kotlin
              kotlinSourceDirectorySet.srcDir(task.get().outputDir)
              // TODO: make something for compileTestKotlin ?
              project.tasks.named("compileKotlin").configure { it.dependsOn(task) }
            }
          }
          generateClassesTask.dependsOn(task)
        }
      }
      else -> {
        // InstantAppExtension or somthing else we don't support yet
        throw IllegalArgumentException("${androidExtension.javaClass.name} is not supported at the moment")
      }
    }
  }

  private fun registerAndroid(project: Project, apolloExtension: ApolloExtension, variant: BaseVariant, generateClassesTask: TaskProvider<Task>) {
    val apolloVariant = ApolloVariant(
        name = variant.name,
        sourceSetNames = variant.sourceSets.map { it.name }.distinct()
    )

    val task = registerVariantTask(project, apolloExtension, apolloVariant) { task ->
      variant.registerJavaGeneratingTask(task.get(), task.get().outputDir)
    }
    generateClassesTask.dependsOn(task)
  }

  private fun registerVariantTask(project: Project,
                                  apolloExtension: ApolloExtension,
                                  apolloVariant: ApolloVariant,
                                  registerCodegenTask: (TaskProvider<ApolloCodegenTask>) -> Unit): TaskProvider<Task> {

    val generateVariantClassesTask = project.tasks.register("generate${apolloVariant.name.capitalize()}ApolloClasses") {
      it.group = TASK_GROUP
    }


    if (apolloExtension.services.all.isEmpty()) {
      apolloExtension.services.all.addAll(servicesForVariant(project, apolloVariant))
    }
    apolloExtension.services.all.forEach { service ->
      fixService(service)
      val task = registerCodegenTasks(project, apolloExtension, apolloVariant, service)
      generateVariantClassesTask.dependsOn(task)
      registerCodegenTask(task)
    }

    return generateVariantClassesTask
  }

  private fun fixService(service: ApolloExtension.Service) {
    if (service.graphqlFilesFolder == null) {
      service.graphqlFilesFolder = service.schemaFilePath.substringBeforeLast("/")
    }

    if (service.include == null) {
      service.include = listOf(".*\\.graphql", ".*\\.gql")
    }
    if (service.packageName == null) {
      service.packageName = service.graphqlFilesFolder!!.substringAfter("/").replace("/", ".")
    }
  }

  private fun servicesForVariant(project: Project, apolloVariant: ApolloVariant): List<ApolloExtension.Service> {
    val graphQLPaths = mutableListOf<String>()
    apolloVariant.sourceSetNames.map {
      findFiles(project.file("src/${it}/graphql"), "schema.json", graphQLPaths, false)
    }
    return graphQLPaths.groupBy {
      project.relativePath(it).substringAfter("/").substringAfter("/")
    }
        .keys
        .mapIndexed { index, key ->
          ApolloExtension.Service("$index").apply {
            schemaFilePath = key
          }
        }
  }

  private fun findFiles(dir: File, fileName: String, output: MutableList<String>, inSubTree: Boolean) {
    if (dir.isDirectory) {
      var sub = inSubTree
      dir.listFiles()!!.forEach {
        if (it.isFile && it.name == fileName) {
          if (inSubTree) {
            throw IllegalArgumentException("""schema files are in the same directory tree:
              |${it.absolutePath}
              |${output.lastOrNull()}
            """.trimMargin())
          }
          sub = true
          output.add(it.absolutePath)
        }
      }
      dir.listFiles()!!.forEach {
        if (it.isDirectory) {
          findFiles(it, fileName, output, sub)
        }
      }
    }
  }

  fun registerCodegenTasks(project: Project, extension: ApolloExtension, apolloVariant: ApolloVariant, service: ApolloExtension.Service): TaskProvider<ApolloCodegenTask> {
    val taskName = "generate${apolloVariant.name.capitalize()}${service.name.capitalize()}ApolloClasses"

    val graphQLFiles = mutableMapOf<String, File>()

    apolloVariant.sourceSetNames.forEach sourceSetName@{ sourceSetName ->
      val dir = project.file("src/$sourceSetName/${service.graphqlFilesFolder}")
      val files = dir.listFiles()
      if (files == null) {
        // happens if no files are in this sourceSet
        return@sourceSetName
      }

      files.forEach file@ { file ->
        if (!file.isFile) {
          return@file
        }

        if (service.exclude?.firstOrNull { Regex(it).matches(file.name) } != null) {
          return@file
        }

        if (service.include!!.firstOrNull { Regex(it).matches(file.name) } == null) {
          return@file
        }

        graphQLFiles.put(file.name, file)
      }
    }

    val allFiles = graphQLFiles.values.toList()

    val schemaFile = apolloVariant.sourceSetNames.map { sourceSetName ->
      project.file("src/$sourceSetName/${service.schemaFilePath}")
    }.lastOrNull {
      it.exists()
    }

    return project.tasks.register(taskName, ApolloCodegenTask::class.java) {
      val outputFolder = File(project.buildDir, "generated/source/apollo/${apolloVariant.name}/${service.name}")

      it.source(allFiles)
      if (schemaFile != null) {
        it.source(schemaFile)
      }

      it.graphqlFiles = allFiles
      it.schemaFile = schemaFile
      it.outputPackageName = service.packageName
      it.group = TASK_GROUP
      it.description = "Generate Android classes for ${apolloVariant.name.capitalize()}${service.name} GraphQL queries"
      it.outputDir = outputFolder
      it.nullableValueType = extension.nullableValueType
      it.useSemanticNaming = extension.useSemanticNaming
      it.generateModelBuilder = extension.generateModelBuilder
      it.useJavaBeansSemanticNaming = extension.useJavaBeansSemanticNaming
      it.suppressRawTypesWarning = extension.suppressRawTypesWarning
      it.generateKotlinModels = extension.generateKotlinModels
      it.generateVisitorForPolymorphicDatatypes = extension.generateVisitorForPolymorphicDatatypes
      it.customTypeMapping = extension.customTypeMapping
    }
  }
}