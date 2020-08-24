package com.apollographql.apollo.gradle.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object JvmTaskConfigurator {

  fun getVariants(project: Project): NamedDomainObjectContainer<ApolloVariant> {
    val container = project.container(ApolloVariant::class.java)

    val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
    val sourceSets = javaPlugin.sourceSets

    sourceSets.map { it.name }.forEach { name ->
      val apolloVariant = ApolloVariant(
          name = name,
          sourceSetNames = listOf(name),
          androidVariant = null,
          isTest = name == "test"
      )

      container.add(apolloVariant)
    }

    return container
  }

  fun registerGeneratedDirectory(project: Project, compilationUnit: DefaultCompilationUnit, codegenProvider: TaskProvider<ApolloGenerateSourcesTask>) {
    val sourceSetName = compilationUnit.variantName

    val sourceDirectorySet = if (compilationUnit.generateKotlinModels()) {
      (project.extensions.getByName("kotlin") as KotlinProjectExtension).sourceSets.getByName(sourceSetName).kotlin
    } else {
      project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName(sourceSetName).java
    }

    val language = if (compilationUnit.generateKotlinModels()) "kotlin" else "java"
    val baseName = if (sourceSetName == "main") {
      ""
    } else {
      sourceSetName
    }

    // This is taken from the Java plugin to try and match their naming.
    // Hopefully the Kotlin plugin uses similar code.
    // See https://github.com/gradle/gradle/blob/v6.1.1/subprojects/plugins/src/main/java/org/gradle/api/internal/tasks/DefaultSourceSet.java#L136
    val compileTaskName = GUtil.toCamelCase("compile $baseName $language", true)

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
      }.configureEach {
        (it as KotlinCompile).source(codegenProvider.get().outputDir.get().asFile)
      }
    }
    sourceDirectorySet.srcDir(codegenProvider.flatMap { it.outputDir })
    project.tasks.named(compileTaskName) { it.dependsOn(codegenProvider) }
  }
}