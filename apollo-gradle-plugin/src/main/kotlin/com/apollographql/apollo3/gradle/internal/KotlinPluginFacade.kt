package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.gradle.api.kotlinMultiplatformExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation

/**
 * A class that hides all references to the Kotlin plugin from the caller.
 * For a non-Kotlin project, this class will never be loaded so that no runtime
 * exception is thrown
 */
fun getKotlinTargetLanguage(project: Project, userSpecified: String?): TargetLanguage {
  return when (userSpecified) {
    "1.4" -> TargetLanguage.KOTLIN_1_4
    "1.5" -> TargetLanguage.KOTLIN_1_5
    null -> {
      // User didn't specify a version: defaults to the Kotlin plugin's version
      val majorMinor = project.getKotlinPluginVersion()!!.take(3)
      if (majorMinor == "1.4") {
        TargetLanguage.KOTLIN_1_4
      } else {
        // For "1.5" *and* unknown (must be higher) versions use "1.5"
        TargetLanguage.KOTLIN_1_5
      }
    }
    else -> error("ApolloGraphQL: languageVersion '$userSpecified' is not supported, must be either '1.4' or '1.5'")
  }
}

internal fun linkSqlite(project: Project) {
  val extension = project.kotlinMultiplatformExtension ?: return
  extension.targets
      .flatMap { it.compilations }
      .filterIsInstance<KotlinNativeCompilation>()
      .forEach { compilationUnit ->
        compilationUnit.kotlinOptions.freeCompilerArgs += arrayOf("-linker-options", "-lsqlite3")
      }
}
