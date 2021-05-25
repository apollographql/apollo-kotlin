package com.apollographql.apollo3.gradle.api

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.gradle.internal.ApolloGenerateSourcesTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object AndroidProject {
  fun onEachVariant(project: Project, withTestVariants: Boolean = false, block: (BaseVariant) -> Unit) {
    val androidExtension = project.androidExtensionOrFail

    val androidVariants = when (androidExtension) {
      is LibraryExtension -> androidExtension.libraryVariants
      is AppExtension -> androidExtension.applicationVariants
      else -> {
        // InstantAppExtension or something else we don't support yet
        throw IllegalArgumentException("${androidExtension.javaClass.name} is not supported at the moment")
      }
    }

    androidVariants.all {
      block(it)
    }

    if (withTestVariants && androidExtension is TestedExtension) {
      androidExtension.testVariants.all {
        block(it)
      }
      androidExtension.unitTestVariants.all {
        block(it)
      }
    }
  }

  fun registerGeneratedDirectory(
      project: Project,
      variant: BaseVariant,
      wire: Service.OutputDirWire
  ) {
    val tasks = project.tasks

    // This doesn't seem to do much besides addJavaSourceFoldersToModel
    // variant.registerJavaGeneratingTask(codegenProvider.get(), codegenProvider.get().outputDir.get().asFile)

    // This is apparently needed for intelliJ to find the generated files
    // TODO: make this lazy (https://github.com/apollographql/apollo-android/issues/1454)
    variant.addJavaSourceFoldersToModel(wire.outputDir.get().asFile)
    // Tell the kotlin compiler to compile our files
    tasks.named("compile${variant.name.capitalizeFirstLetter()}Kotlin").configure {
      it.dependsOn(wire.task)
      (it as KotlinCompile).source(wire.outputDir.get())
    }
  }

  fun registerGeneratedDirectoryToAllVariants(
      project: Project,
      wire: Service.OutputDirWire,
  ) {
    onEachVariant(project) { variant ->
      registerGeneratedDirectory(project, variant, wire)
    }
  }
}
