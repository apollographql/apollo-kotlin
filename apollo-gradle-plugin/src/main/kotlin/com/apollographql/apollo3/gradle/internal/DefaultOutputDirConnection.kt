package com.apollographql.apollo3.gradle.internal

import com.android.build.gradle.AppExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.gradle.api.Service
import com.apollographql.apollo3.gradle.api.androidExtensionOrThrow
import com.apollographql.apollo3.gradle.api.applicationVariants
import com.apollographql.apollo3.gradle.api.javaConvention
import com.apollographql.apollo3.gradle.api.javaConventionOrThrow
import com.apollographql.apollo3.gradle.api.kotlinProjectExtensionOrThrow
import com.apollographql.apollo3.gradle.api.libraryVariants
import com.apollographql.apollo3.gradle.api.testVariants
import com.apollographql.apollo3.gradle.api.unitTestVariants
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_JS_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal class DefaultOutputDirConnection(
    private val project: Project,
    override val task: TaskProvider<out Task>,
    override val outputDir: Provider<Directory>
): Service.OutputDirConnection {
  override fun connectToKotlinSourceSet(name: String) {
    project.kotlinProjectExtensionOrThrow.sourceSets.getByName(name).kotlin.srcDir(outputDir)
  }

  override fun connectToJavaSourceSet(name: String) {
    project.javaConventionOrThrow
        .sourceSets
        .getByName(name)
        .java
        .srcDir(outputDir)
  }

  private fun Any.getConvention(name: String): Any? =
      (this as HasConvention).convention.plugins[name]

  // Copied from kotlin plugin
  private val AndroidSourceSet.kotlinSourceSet: SourceDirectorySet?
    get() {
      val convention = (getConvention(KOTLIN_DSL_NAME) ?: getConvention(KOTLIN_JS_DSL_NAME)) ?: return null
      val kotlinSourceSetIface =
          convention.javaClass.interfaces.find { it.name == KotlinSourceSet::class.qualifiedName }
      val getKotlin = kotlinSourceSetIface?.methods?.find { it.name == "getKotlin" } ?: return null
      return getKotlin(convention) as? SourceDirectorySet
    }

  override fun connectToAndroidVariant(variant: BaseVariant) {
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
    connectToAndroidSourceSet(sourceSetName)
  }

  override fun connectToAndroidSourceSet(name: String) {
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

    container.all {
      if (it.sourceSets.any { it.name == name }) {
        // This is required for AS to see the sources
        // See https://github.com/apollographql/apollo-android/issues/3351
        it.addJavaSourceFoldersToModel(outputDir.get().asFile)
      }
    }

    project.androidExtensionOrThrow
        .sourceSets
        .getByName(name)
        .kotlinSourceSet!!
        .srcDir(outputDir)
  }
}