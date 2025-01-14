package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.gradle.api.Service
import com.apollographql.apollo.gradle.internal.DefaultApolloExtension.Companion.hasKotlinPlugin
import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * A class that hides all references to the Kotlin plugin from the caller.
 * For a non-Kotlin project, this class will never be loaded so that no runtime
 * exception is thrown
 */
fun getKotlinTargetLanguage(kgpVersion: String, userSpecified: String?): TargetLanguage {
  @Suppress("DEPRECATION")
  return when (userSpecified) {
    "1.5" -> TargetLanguage.KOTLIN_1_5
    "1.9" -> TargetLanguage.KOTLIN_1_9
    null -> {
      // User didn't specify a version: default to the Kotlin plugin version
      val kotlinPluginVersion = kgpVersion.split("-")[0]
      val versionNumbers = kotlinPluginVersion.split(".").map { it.toInt() }
      val version = KotlinVersion(versionNumbers[0], versionNumbers[1])
      if (version.isAtLeast(1, 9)) {
        TargetLanguage.KOTLIN_1_9
      } else {
        TargetLanguage.KOTLIN_1_5
      }
    }

    else -> error("Apollo: languageVersion '$userSpecified' is not supported, Supported values: '1.5', '1.9'")
  }
}

fun Project.apolloGetKotlinPluginVersion(): String? {
  if (!project.hasKotlinPlugin()) {
    return null
  }

  return project.getKotlinPluginVersion()
}

/*
 * Inspired by SQLDelight:
 * https://github.com/sqldelight/sqldelight/blob/ae8c348f6cf76822828bc65832106ec151ca5b6c/sqldelight-gradle-plugin/src/main/kotlin/app/cash/sqldelight/gradle/kotlin/LinkSqlite.kt#L7
 * https://github.com/sqldelight/sqldelight/issues/1442
 *
 * Ideally this can be forwarded automatically, but I don't think this can be done as of today.
 */
internal fun linkSqlite(project: Project) {
  val extension = project.kotlinMultiplatformExtension ?: return

  extension.targets.filterIsInstance<KotlinNativeTarget>()
      .flatMap { it.binaries }
      .forEach { it.linkerOpts("-lsqlite3") }
}

internal fun checkKotlinPluginVersion(project: Project) {
  val version = project.getKotlinPluginVersion()
      .split(".")
      .take(2)
      .map { it.toInt() }

  val isKotlinSupported = when {
    version[0] > 1 -> true
    version[0] == 1 -> version[1] >= 5
    else -> false
  }
  require(isKotlinSupported) {
    "Apollo Kotlin requires Kotlin plugin version 1.5 or more (found '${project.getKotlinPluginVersion()}')"
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
      @Suppress("DEPRECATION")
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
