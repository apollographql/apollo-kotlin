package com.apollographql.apollo.gradle.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object JvmTaskConfigurator {

  fun getVariants(project: Project): NamedDomainObjectContainer<ApolloVariant> {
    val container = project.container(ApolloVariant::class.java)

    val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
    val sourceSets = javaPlugin.sourceSets

    sourceSets.map { it.name }.forEach { name ->
      val apolloVariant = JvmApolloVariant(
          name = name
      )
      container.add(apolloVariant)
    }

    return container
  }
}

class JvmApolloVariant(name: String) : ApolloVariant(name, listOf(name), null) {
  override fun registerGeneratedDirectory(project: Project, forKotlin: Boolean, codegenProvider: TaskProvider<ApolloGenerateSourcesTask>) {
    val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
    val sourceSets = javaPlugin.sourceSets
    val sourceSetName = name

    var taskName = sourceSetName.capitalize()
    if (taskName == "Main") {
      // Special case: The main variant will use "compileJava" and not "compileMainJava"
      taskName = ""
    }

    val sourceDirectorySet = if (!forKotlin) {
      sourceSets.getByName(sourceSetName).java
    } else {
      sourceSets.getByName(sourceSetName).kotlin!!
    }

    val compileTaskName = if (!forKotlin) {
      "compile${taskName}Java"
    } else {
      "compile${taskName}Kotlin"
    }

    if (!forKotlin) {
      /**
       * By the time we come here, the KotlinCompile task has been configured by the kotlin plugin already.
       *
       * Right now this is done in [org.jetbrains.kotlin.gradle.plugin.AbstractAndroidProjectHandler.configureSources].
       *
       * To workaround this, we're adding the java generated models folder here
       */
      project.tasks.matching {
        it.name == "compileKotlin"
      }.configureEach {
        (it as KotlinCompile).source(codegenProvider.get().outputDir.get().asFile)
      }
    }
    sourceDirectorySet.srcDir(codegenProvider.flatMap { it.outputDir })
    project.tasks.named(compileTaskName) { it.dependsOn(codegenProvider) }
  }
}