package com.apollographql.apollo.gradle.internal

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object AndroidTaskConfigurator {
  // TODO: make this lazy (https://github.com/apollographql/apollo-android/issues/1454)
  fun registerGeneratedDirectory(
      tasks: TaskContainer,
      androidExtension: BaseExtension,
      codegenProvider: TaskProvider<ApolloGenerateSourcesTask>
  ) {
    val androidVariants = when (androidExtension) {
      is LibraryExtension -> androidExtension.libraryVariants
      is AppExtension -> androidExtension.applicationVariants
      else -> {
        // InstantAppExtension or something else we don't support yet
        throw IllegalArgumentException("${androidExtension.javaClass.name} is not supported at the moment")
      }
    }
    androidVariants.forEach { variant ->
      // This doesn't seem to do much besides addJavaSourceFoldersToModel
      // variant.registerJavaGeneratingTask(codegenProvider.get(), codegenProvider.get().outputDir.get().asFile)
      // This is apparently needed for intelliJ to find the generated files
      variant.addJavaSourceFoldersToModel(codegenProvider.get().outputDir.get().asFile)
      // Tell the kotlin compiler to compile our files
      tasks.named("compile${variant.name.capitalize()}Kotlin").configure {
        it.dependsOn(codegenProvider)
        (it as KotlinCompile).source(codegenProvider.get().outputDir.asFile.get())
      }
    }
  }
}
