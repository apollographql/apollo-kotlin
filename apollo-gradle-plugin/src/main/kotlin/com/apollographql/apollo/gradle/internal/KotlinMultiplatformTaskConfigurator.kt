package com.apollographql.apollo.gradle.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

object KotlinMultiplatformTaskConfigurator {

  fun getVariants(project: Project): NamedDomainObjectContainer<ApolloVariant> {
    val container = project.container(ApolloVariant::class.java)

    project.kotlinMultiplatformExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) {
      val apolloVariant = ApolloVariant(
          name = it.name,
          sourceSetNames = listOf(it.name),
          androidVariant = null,
          isTest = false
      )
      container.add(apolloVariant)
    }
    project.kotlinMultiplatformExtension.sourceSets.getByName(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME) {
      val apolloVariant = ApolloVariant(
          name = it.name,
          sourceSetNames = listOf(it.name),
          androidVariant = null,
          isTest = true
      )
      container.add(apolloVariant)
    }

    return container
  }

  fun registerGeneratedDirectory(project: Project, compilationUnit: DefaultCompilationUnit, codeGenProvider: TaskProvider<ApolloGenerateSourcesTask>) {
    val variant = compilationUnit.apolloVariant

    val sourceDirectorySet = project.kotlinMultiplatformExtension.sourceSets.getByName(variant.name).kotlin
    val baseName = if (variant.isTest) "Test" else ""

    sourceDirectorySet.srcDir(codeGenProvider.flatMap { it.outputDir })
    project.tasks.matching { it.name.startsWith("compile${baseName}Kotlin") }.configureEach {
      it.dependsOn(codeGenProvider)
    }
  }

  private val Project.kotlinMultiplatformExtension
    get() = extensions.getByName("kotlin") as KotlinMultiplatformExtension
}
