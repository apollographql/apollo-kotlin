package com.apollographql.apollo.gradle.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider

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

    val sourceDirectorySet = if (!compilationUnit.compilerParams.generateKotlinModels.get()) {
      sourceSets.getByName(name).java
    } else {
      sourceSets.getByName(name).kotlin!!
    }

    val compileTaskName = if (!compilationUnit.compilerParams.generateKotlinModels.get()) {
      "compileJava"
    } else {
      "compileKotlin"
    }
    sourceDirectorySet.srcDir(codegenProvider.flatMap { it.outputDir })
    project.tasks.named(compileTaskName) { it.dependsOn(codegenProvider) }
  }
}