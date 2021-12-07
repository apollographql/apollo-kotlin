package com.apollographql.apollo3.gradle.internal

import com.android.build.gradle.AppExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.gradle.api.androidExtensionOrThrow
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

fun connectToAndroidSourceSet(project: Project, sourceSetName: String, outputDir: Provider<Directory>, task: Task) {
  val container = project.container(BaseVariant::class.java)

  val extension = project.androidExtensionOrThrow
  when (extension) {
    is LibraryExtension -> {
      extension.libraryVariants.all { variant ->
        container.add(variant)
      }
    }
    is AppExtension -> {
      extension.applicationVariants.all { variant ->
        container.add(variant)
      }
    }
    is FeatureExtension -> {
      extension.featureVariants.all { variant ->
        container.add(variant)
      }
    }
    else -> error("Unsupported extension: $extension")
  }

  if (extension is TestedExtension) {
    extension.testVariants.all { variant ->
      container.add(variant)
    }
    extension.unitTestVariants.all { variant ->
      container.add(variant)
    }
  }

  val androidSourceSet = project.androidExtensionOrThrow
      .sourceSets
      .getByName(sourceSetName)

  val kotlinSourceSet = androidSourceSet.kotlinSourceSet()
  if (kotlinSourceSet != null) {
    kotlinSourceSet.srcDir(outputDir)
  }

  container.all {
    if (it.sourceSets.any { it.name == sourceSetName }) {
      if (kotlinSourceSet == null) {
        it.registerJavaGeneratingTask(task, outputDir.get().asFile)
      } else {
        // The kotlinSourceSet carries task dependencies, calling srcDir() above is enough
        // to setup task dependencies
        // addJavaSourceFoldersToModel is still required for AS to see the sources
        // See https://github.com/apollographql/apollo-android/issues/3351
        it.addJavaSourceFoldersToModel(outputDir.get().asFile)
      }
    }
  }
}

fun connectToAndroidVariant(project: Project, variant: Any, outputDir: Provider<Directory>, task: Task) {
  check(variant is BaseVariant) {
    "Apollo: 'variant' must be an instance of an Android [BaseVariant]"
  }
  /**
   * Heuristic to get the variant-specific sourceSet from the variant name
   * demoDebugAndroidTest -> androidTestDemoDebug
   * demoDebugUnitTest -> testDemoDebug
   * demoDebug -> demoDebug
   */
  val sourceSetName = when {
    variant is TestVariant && variant.name.endsWith("AndroidTest") -> {
      "androidTest${variant.name.removeSuffix("AndroidTest").capitalizeFirstLetter()}"
    }
    variant is UnitTestVariant && variant.name.endsWith("UnitTest") -> {
      "test${variant.name.removeSuffix("UnitTest").capitalizeFirstLetter()}"
    }
    else -> variant.name
  }

  connectToAndroidSourceSet(project, sourceSetName, outputDir, task)
}
