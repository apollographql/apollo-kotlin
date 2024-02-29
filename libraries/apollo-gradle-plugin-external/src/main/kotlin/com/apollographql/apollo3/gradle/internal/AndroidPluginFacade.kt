/**
 * TODO: Figure out a way to make it work with the new AGP 8.0.0 variant APIs.
 * See https://issuetracker.google.com/issues/327399383
 *
 * When doing so it might interesting to refactor this code so that classes referencing possibly absent symbols are not loaded if not needed
 * See https://chromium.googlesource.com/chromium/src/+/HEAD/build/android/docs/class_verification_failures.md for an Android link that does
 * not apply here but gives a good description of the potential issue.
 */
@file:Suppress("DEPRECATION")

package com.apollographql.apollo3.gradle.internal

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

private fun Project.getVariants(): NamedDomainObjectContainer<BaseVariant> {
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

  @Suppress("USELESS_IS_CHECK", "KotlinRedundantDiagnosticSuppress")
  if (extension is TestedExtension) {
    extension.testVariants.configureEach { variant ->
      container.add(variant)
    }
    extension.unitTestVariants.configureEach { variant ->
      container.add(variant)
    }
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

  project.getVariants().configureEach {
    if (it.sourceSets.any { it.name == sourceSetName }) {
      if (kotlinSourceSet == null) {
        it.registerJavaGeneratingTask(taskProvider, outputDir.get().asFile)
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

/**
 * This uses https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/api/BaseVariant.java;l=539;drc=5ac687029454dec1e7bd50697dabbc24d5a9943c
 *
 * There's a newer API in 7.3 where AGP decides where the sources are put but we're not using that just yet
 * https://github.com/android/gradle-recipes/blob/agp-7.3/Kotlin/addJavaSourceFromTask/app/build.gradle.kts
 */
private val lazyRegisterJavaGeneratingTask: Method? = BaseVariant::class.java.declaredMethods.singleOrNull {
  if (it.name != "registerJavaGeneratingTask") {
    return@singleOrNull false
  }

  if (it.parameters.size != 2) {
    return@singleOrNull false
  }
  val parameter0Type = it.parameters[0].parameterizedType
  if (parameter0Type !is ParameterizedType) {
    return@singleOrNull false
  }
  if (parameter0Type.rawType.typeName != "org.gradle.api.tasks.TaskProvider") {
    return@singleOrNull false
  }
  val parameter1Type = it.parameters[1].parameterizedType
  if (parameter1Type !is ParameterizedType) {
    return@singleOrNull false
  }
  if (parameter1Type.rawType.typeName != "java.util.Collection") {
    return@singleOrNull false
  }
  if (parameter1Type.actualTypeArguments.size != 1) {
    return@singleOrNull false
  }
  if (parameter1Type.actualTypeArguments.single().typeName != "java.io.File") {
    return@singleOrNull false
  }

  true
}

fun connectToAndroidVariant(project: Project, variant: Any, outputDir: Provider<Directory>, taskProvider: TaskProvider<out Task>) {
  check(variant is BaseVariant) {
    "Apollo: variant must be an instance of com.android.build.gradle.api.BaseVariant (found $variant)"
  }

  if (lazyRegisterJavaGeneratingTask != null) {
    lazyRegisterJavaGeneratingTask.invoke(variant, taskProvider, listOf(outputDir.get().asFile))
  } else {
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

    connectToAndroidSourceSet(project, sourceSetName, outputDir, taskProvider)
  }
}

fun connectToAllAndroidVariants(project: Project, outputDir: Provider<Directory>, taskProvider: TaskProvider<out Task>) {
  if (lazyRegisterJavaGeneratingTask != null) {
    project.getVariants().configureEach {
      lazyRegisterJavaGeneratingTask.invoke(it, taskProvider, listOf(outputDir.get().asFile))
    }
  } else {
    connectToAndroidSourceSet(project, "main", outputDir, taskProvider)
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
