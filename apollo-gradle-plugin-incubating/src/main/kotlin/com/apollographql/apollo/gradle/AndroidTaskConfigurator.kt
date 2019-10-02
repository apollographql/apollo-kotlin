package com.apollographql.apollo.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo.gradle.api.ApolloExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

object AndroidTaskConfigurator {
  fun configure(apolloExtension: ApolloExtension,
                androidExtension: Any,
                project: Project,
                registerVariantTask: (Project, ApolloVariant, block: (TaskProvider<ApolloGenerateSourcesTask>) -> Unit) -> Unit
  ) {
    when {
      androidExtension is LibraryExtension -> {
        androidExtension.libraryVariants.all(Action { variant ->
          registerAndroid(project, apolloExtension, androidExtension, variant, registerVariantTask)
        })
        // TODO: add test variants ?
      }
      androidExtension is AppExtension -> {
        androidExtension.applicationVariants.all(Action { variant ->
          registerAndroid(project, apolloExtension, androidExtension, variant, registerVariantTask)
        })
        // TODO: add test variants ?
      }
      else -> {
        // InstantAppExtension or something else we don't support yet
        throw IllegalArgumentException("${androidExtension.javaClass.name} is not supported at the moment")
      }
    }
  }

  private fun registerAndroid(project: Project,
                              apolloExtension: ApolloExtension,
                              androidExtension: BaseExtension,
                              variant: BaseVariant,
                              registerVariantTask: (Project, ApolloVariant, block: (TaskProvider<ApolloGenerateSourcesTask>) -> Unit) -> Unit) {

    val apolloVariant = ApolloVariant(
        name = variant.name,
        sourceSetNames = variant.sourceSets.map { it.name }.distinct(),
        androidVariant = variant
    )

    registerVariantTask(project, apolloVariant) { generateSourcesTaskProvider ->
      if (apolloExtension.generateKotlinModels) {
        variant.addJavaSourceFoldersToModel(generateSourcesTaskProvider.get().outputDir.get().asFile)
        androidExtension.sourceSets.first { it.name == variant.name }.kotlin!!.srcDir(generateSourcesTaskProvider.get().outputDir)
        project.tasks.named("compile${variant.name.capitalize()}Kotlin").configure { it.dependsOn(generateSourcesTaskProvider) }
      } else {
        variant.registerJavaGeneratingTask(generateSourcesTaskProvider.get(), generateSourcesTaskProvider.get().outputDir.get().asFile)
      }
    }
  }
}