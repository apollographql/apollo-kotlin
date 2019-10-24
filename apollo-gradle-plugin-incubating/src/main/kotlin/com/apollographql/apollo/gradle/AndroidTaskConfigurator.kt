package com.apollographql.apollo.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo.gradle.api.ApolloExtension
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

object AndroidTaskConfigurator {
  fun getVariants(project: Project, androidExtension: Any): NamedDomainObjectContainer<ApolloVariant> {
    val container = project.container(ApolloVariant::class.java)

    when {
      androidExtension is LibraryExtension -> {
        // TODO: add test variants ?
        androidExtension.libraryVariants.all { variant ->
          container.add(ApolloVariant(
              name = variant.name,
              sourceSetNames = variant.sourceSets.map { it.name }.distinct(),
              androidVariant = variant
          ))
        }
      }
      androidExtension is AppExtension -> {
        // TODO: add test variants ?
        androidExtension.applicationVariants.all { variant ->
          container.add(ApolloVariant(
              name = variant.name,
              sourceSetNames = variant.sourceSets.map { it.name }.distinct(),
              androidVariant = variant
          ))
        }
      }
      else -> {
        // InstantAppExtension or something else we don't support yet
        throw IllegalArgumentException("${androidExtension.javaClass.name} is not supported at the moment")
      }
    }
    return container
  }

  fun registerGeneratedDirectory(
      project: Project,
      androidExtension: Any,
      compilationUnit: DefaultCompilationUnit,
      codegenProvider: TaskProvider<ApolloGenerateSourcesTask>
  ) {

    val variant = compilationUnit.androidVariant as BaseVariant
    if (compilationUnit.compilerParams.generateKotlinModels) {
      variant.addJavaSourceFoldersToModel(codegenProvider.get().outputDir.get().asFile)
      androidExtension as BaseExtension
      // TODO: make this lazy
      androidExtension.sourceSets.first { it.name == variant.name }.kotlin!!.srcDir(codegenProvider.get().outputDir)
      project.tasks.named("compile${variant.name.capitalize()}Kotlin").configure { it.dependsOn(codegenProvider) }
    } else {
      // TODO: make this lazy
      variant.registerJavaGeneratingTask(codegenProvider.get(), codegenProvider.get().outputDir.get().asFile)
    }
  }
}