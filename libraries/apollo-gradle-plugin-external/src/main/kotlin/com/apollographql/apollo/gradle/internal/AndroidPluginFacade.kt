/**
 * TODO: Figure out a way to make it work with the new AGP 8.0.0 variant APIs.
 * See https://issuetracker.google.com/issues/327399383
 *
 * When doing so it might interesting to refactor this code so that classes referencing possibly absent symbols are not loaded if not needed.
 *
 * For an example, the IJ plugin calls AndroidProjectKt.androidExtension whose return type is a `BaseExtension?`. I'm not sure how come
 * AndroidProjectKt links given BaseExtension is not always in the classpath.
 * See https://chromium.googlesource.com/chromium/src/+/HEAD/build/android/docs/class_verification_failures.md for an Android link that does
 * not apply here but gives a good description of the potential issue.
 */
@file:Suppress("DEPRECATION")

package com.apollographql.apollo.gradle.internal

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

private fun Project.getMainVariants(): NamedDomainObjectContainer<BaseVariant> {
  val container = project.container(BaseVariant::class.java)

  val extension: BaseExtension = project.androidExtensionOrThrow
  when (extension) {
    is LibraryExtension -> {
      extension.libraryVariants.configureEach { variant ->
        container.add(variant)
      }
    }

    is AppExtension -> {
      extension.applicationVariants.configureEach { variant ->
        container.add(variant)
      }
    }

    else -> error("Unsupported extension: $extension")
  }

  return container
}

fun connectToAndroidSourceSet(
    project: Project,
    sourceSetName: String,
    outputDir: Provider<Directory>,
    taskProvider: TaskProvider<out Task>,
) {
  val kotlinSourceSet = project.kotlinProjectExtension?.sourceSets?.getByName(sourceSetName)?.kotlin
  if (kotlinSourceSet != null) {
    kotlinSourceSet.srcDir(outputDir)
  }

  project.getMainVariants().configureEach {
    if (it.sourceSets.any { it.name == sourceSetName }) {
      if (kotlinSourceSet == null) {
        it.registerJavaGeneratingTask(taskProvider, outputDir.get().asFile)
      } else {
        // The kotlinSourceSet carries task dependencies, calling srcDir() above is enough
        // to setup task dependencies
        // addJavaSourceFoldersToModel is still required for AS to see the sources
        // See https://github.com/apollographql/apollo-kotlin/issues/3351
        it.addJavaSourceFoldersToModel(outputDir.get().asFile)
      }
    }
  }
}

fun connectToAndroidVariant(variant: Any, outputDir: Provider<Directory>, taskProvider: TaskProvider<out Task>) {
  check(variant is BaseVariant) {
    "Apollo: variant must be an instance of com.android.build.gradle.api.BaseVariant (found $variant)"
  }

  variant.registerJavaGeneratingTask(taskProvider, listOf(outputDir.get().asFile))
}

fun connectToAllAndroidVariants(project: Project, outputDir: Provider<Directory>, taskProvider: TaskProvider<out Task>) {
  project.getMainVariants().configureEach {
    connectToAndroidVariant(it, outputDir, taskProvider)
  }
}

internal val BaseExtension.minSdk: Int?
  get() = defaultConfig.minSdkVersion?.apiLevel

internal val BaseExtension.targetSdk: Int?
  get() = defaultConfig.targetSdkVersion?.apiLevel

/**
 * BaseExtension is used as a receiver here to make sure we do not try to call this
 * code if AGP is not in the classpath
 */
internal val BaseExtension.pluginVersion: String
  get() = com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
