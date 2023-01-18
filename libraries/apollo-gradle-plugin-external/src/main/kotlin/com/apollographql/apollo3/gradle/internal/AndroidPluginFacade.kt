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
import com.apollographql.apollo3.gradle.api.kotlinProjectExtension
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

private fun Project.getVariants(): NamedDomainObjectContainer<BaseVariant> {
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

  project.getVariants().all {
    if (it.sourceSets.any { it.name == sourceSetName }) {
      if (kotlinSourceSet == null) {
        try {
          // AGP 7.0.0+: do things lazily
          it.javaClass.getMethod("registerJavaGeneratingTask", TaskProvider::class.java, Array<File>::class.java)
              .invoke(it, taskProvider, arrayOf(outputDir.get().asFile))
        } catch (e: Exception) {
          // Older AGP: do things eagerly
          it.registerJavaGeneratingTask(taskProvider.get(), outputDir.get().asFile)
        }
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
    "Apollo: variant must be an instance of an Android BaseVariant (found $variant)"
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
    project.getVariants().all {
      lazyRegisterJavaGeneratingTask.invoke(it, taskProvider, listOf(outputDir.get().asFile))
    }
  } else {
    connectToAndroidSourceSet(project, "main", outputDir, taskProvider)
  }
}