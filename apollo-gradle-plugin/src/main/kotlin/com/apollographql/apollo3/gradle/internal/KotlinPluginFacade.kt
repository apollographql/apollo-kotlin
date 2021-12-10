package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.gradle.api.Service
import com.apollographql.apollo3.gradle.api.kotlinMultiplatformExtension
import com.apollographql.apollo3.gradle.api.kotlinProjectExtensionOrThrow
import org.gradle.api.Action
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
    else -> error("Apollo: languageVersion '$userSpecified' is not supported, must be either '1.4' or '1.5'")
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

internal fun checkKotlinPluginVersion(project: Project) {
  val version = project.getKotlinPluginVersion()!!
      .split(".")
      .take(2)
      .map { it.toInt() }

  val isKotlinSupported = when {
    version[0] > 1 -> true
    version[0] == 1 -> version[1] >= 4
    else -> false
  }
  require(isKotlinSupported) {
    "Apollo Kotlin requires Kotlin plugin version 1.4 or more (found '${project.getKotlinPluginVersion()}')"
  }
}

fun createAllKotlinSourceSetServices(
    apolloExtension: DefaultApolloExtension,
    project: Project,
    sourceFolder: String,
    nameSuffix: String,
    action: Action<Service>,
) {
  project.kotlinProjectExtensionOrThrow.sourceSets.forEach { kotlinSourceSet ->
    val name = "${kotlinSourceSet.name}${nameSuffix.capitalizeFirstLetter()}"

    apolloExtension.service(name) { service ->
      action.execute(service)
      check(!service.sourceFolder.isPresent) {
        "Apollo: service.sourceFolder is not used when calling createAllKotlinJvmSourceSetServices. Use the parameter instead"
      }
      service.srcDir("src/${kotlinSourceSet.name}/graphql/$sourceFolder")
      (service as DefaultService).outputDirAction = Action<Service.DirectoryConnection> { connection ->
        kotlinSourceSet.kotlin.srcDir(connection.outputDir)
      }
    }
  }
}
