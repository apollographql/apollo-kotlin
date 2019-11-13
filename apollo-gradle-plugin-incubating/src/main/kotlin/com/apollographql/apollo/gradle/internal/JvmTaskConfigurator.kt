package com.apollographql.apollo.gradle.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object JvmTaskConfigurator {

  fun getVariants(project: Project): NamedDomainObjectContainer<ApolloVariant> {
    val container = project.container(ApolloVariant::class.java)

    // TODO: should we add tasks for the test sourceSet ?
    val name = "main"
    val apolloVariant = ApolloVariant(
        name = name,
        sourceSetNames = listOf(name),
        androidVariant = null
    )

    container.add(apolloVariant)
    return container
  }

  fun registerGeneratedDirectory(project: Project, compilationUnit: DefaultCompilationUnit, codegenProvider: TaskProvider<ApolloGenerateSourcesTask>) {
    val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
    val sourceSets = javaPlugin.sourceSets
    val name = compilationUnit.variantName

    val sourceDirectorySet = if (!compilationUnit.generateKotlinModels()) {
      sourceSets.getByName(name).java
    } else {
      sourceSets.getByName(name).kotlin!!
    }

    val compileTaskName = if (!compilationUnit.generateKotlinModels()) {
      "compileJava"
    } else {
      "compileKotlin"
    }

    if (!compilationUnit.generateKotlinModels()) {
      /**
       * By the time we come here, the KotlinCompile task has been configured by the kotlin plugin already.
       *
       * Right now this is done in [org.jetbrains.kotlin.gradle.plugin.AbstractAndroidProjectHandler.configureSources].
       *
       * To workaround this, we're adding the java generated models folder here
       */
      project.tasks.matching {
        it.name == "compileKotlin"
      }.configureEach{
        (it as KotlinCompile).source(codegenProvider.get().outputDir.get().asFile)
      }
    }
    sourceDirectorySet.srcDir(codegenProvider.flatMap { it.outputDir })
    project.tasks.named(compileTaskName) { it.dependsOn(codegenProvider) }
  }
}