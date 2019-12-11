package com.apollographql.apollo.gradle.internal

import com.android.build.gradle.BasePlugin
import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.ApolloSourceSetExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
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

    fun registerCodeGenTask(project: Project,
                            compilationUnit: DefaultCompilationUnit,
                            hasUserDefinedServices: () -> Boolean
    ): TaskProvider<ApolloGenerateSourcesTask> {
      val taskName = "generate${compilationUnit.name.capitalize()}ApolloSources"

      return project.tasks.register(taskName, ApolloGenerateSourcesTask::class.java) {
        it.group = TASK_GROUP
        it.description = "Generate Apollo models for ${compilationUnit.name.capitalize()} GraphQL queries"

        val compilerParams = compilationUnit
            .withFallback(project.objects, compilationUnit.service)
            .withFallback(project.objects, compilationUnit.apolloExtension)

        val generateKotlinModels = compilerParams.generateKotlinModels.getOrElse(false)

        /**
         * To avoid project.afterEvaluate, all tasks are registered before we know if the user configured
         * services or set generateKotlinModels. Here we disable the ones that do not make sense once we
         * know the actual user configuration.
         */
        var enabled = true
        if (generateKotlinModels != compilationUnit.kotlin) {
          enabled = false
        }
        if (!compilationUnit.service.isUserDefined && hasUserDefinedServices()) {
          enabled = false
        }

        it.enabled = enabled

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
        it.generateKotlinModels.set(generateKotlinModels)
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

    private fun registerDownloadSchemaTask(project: Project, service: DefaultService) {
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

    fun registerDownloadApolloSchemaTask(project: Project) {
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
    }
  }

  val services = mutableListOf<DefaultService>()
  val variants = mutableListOf<ApolloVariant>()
  val languages = mutableListOf<Language>()

  enum class Language {
    Java,
    Kotlin
  }

  var hasUserDefinedServices = false

  val variantTaskProvider = mutableMapOf<String, TaskProvider<Task>>()

  lateinit var rootTaskProvider: TaskProvider<Task>

  override fun apply(project: Project) {
    require(GradleVersion.current().compareTo(GradleVersion.version("5.6")) >= 0) {
      "apollo-android requires Gradle version 5.6 or greater"
    }

    val apolloExtension = project.extensions.create(ApolloExtension::class.java, "apollo", DefaultApolloExtension::class.java, project) as DefaultApolloExtension
    // for backward compatibility
    val apolloSourceSetExtension = (apolloExtension as ExtensionAware).extensions.create("sourceSet", ApolloSourceSetExtension::class.java, project.objects)

    // the extension block has not been evaluated yet, register a callback once the project has been evaluated
    project.afterEvaluate {
      afterEvaluate(it, apolloExtension, apolloSourceSetExtension)
    }

    registerDownloadApolloSchemaTask(project)

    rootTaskProvider = project.tasks.register("generateApolloSources") {
      it.group = TASK_GROUP
    }

    apolloExtension.services.all { service ->
      onService(project, apolloExtension, service)
    }

    /**
     * Add the default DefaultService.
     */
    apolloExtension.service("service", Action {
      it.isUserDefined = false
    })

    project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.all {
      if (!languages.contains(Language.Java)) {
        onLanguage(project, apolloExtension, Language.Java)
      }

      val apolloVariant = JvmApolloVariant(it.name)
      onVariant(project, apolloExtension, apolloVariant)
    }

    project.plugins.all {
      val androidExtension = project.extensions.findByName("android")
      if (!languages.contains(Language.Java) && androidExtension != null) {
        onLanguage(project, apolloExtension, Language.Java)

        AndroidTaskConfigurator.getVariants(project, androidExtension).all { variant ->
          onVariant(project, apolloExtension, variant)
        }
      }

      if (!languages.contains(Language.Kotlin) && project.extensions.findByName("kotlin") != null) {
        onLanguage(project, apolloExtension, Language.Kotlin)
      }
    }
  }

  private fun onService(project: Project, apolloExtension: DefaultApolloExtension, service: DefaultService) {
    println("onService: ${service.name} - isUserDefined=${service.isUserDefined}")

    registerDownloadSchemaTask(project, service)

    if (service.isUserDefined) {
      hasUserDefinedServices = true
    }

    variants.forEach { variant ->
      languages.forEach { language ->
        registerCodeGenTask(project, apolloExtension, variant, service, language)
      }
    }
    services.add(service)
  }

  private fun onVariant(project: Project, apolloExtension: DefaultApolloExtension, variant: ApolloVariant) {
    println("onVariant: ${variant.name} - ${variant.androidVariant}")
    val taskProvider = project.tasks.register("generate${variant.name.capitalize()}ApolloSources") {
      it.group = TASK_GROUP
    }
    variantTaskProvider.put(variant.name, taskProvider)

    services.forEach { service ->
      languages.forEach { language ->
        registerCodeGenTask(project, apolloExtension, variant, service, language)
      }
    }
    variants.add(variant)
  }

  private fun onLanguage(project: Project, apolloExtension: DefaultApolloExtension, language: Language) {
    println("onLanguage: ${language.name}")

    services.forEach { service ->
      variants.forEach { variant ->
        registerCodeGenTask(project, apolloExtension, variant, service, language)
      }
    }
    languages.add(language)
  }

  private fun registerCodeGenTask(project: Project,
                                  apolloExtension: DefaultApolloExtension,
                                  variant: ApolloVariant,
                                  service: DefaultService,
                                  language: Language) {

    val kotlin = language == Language.Kotlin
    val compilationUnit = DefaultCompilationUnit.createDefaultCompilationUnit(project, apolloExtension, variant, service, kotlin)
    val taskProvider = registerCodeGenTask(project, compilationUnit, { hasUserDefinedServices })

    variant.registerGeneratedDirectory(project, kotlin, taskProvider)

    compilationUnit.outputDir.set(taskProvider.flatMap { it.outputDir })
    compilationUnit.transformedQueriesDir.set(taskProvider.flatMap { it.transformedQueriesOutputDir })

    apolloExtension.compilationUnits.add(compilationUnit)

    rootTaskProvider.configure {
      it.dependsOn(taskProvider)
    }

    variantTaskProvider.get(variant.name)?.configure {
      it.dependsOn(taskProvider)
    }
  }
}
