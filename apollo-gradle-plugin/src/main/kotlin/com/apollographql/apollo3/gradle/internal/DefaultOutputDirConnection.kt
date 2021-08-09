package com.apollographql.apollo3.gradle.internal

import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.gradle.api.Service
import com.apollographql.apollo3.gradle.api.androidExtensionOrThrow
import com.apollographql.apollo3.gradle.api.applicationVariants
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
    project.convention.getByType(JavaPluginConvention::class.java)
        .sourceSets
        .getByName(name)
        .allJava
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
    project.androidExtensionOrThrow
        .sourceSets
        .getByName(name)
        .kotlinSourceSet!!
        .srcDir(outputDir)
  }
}