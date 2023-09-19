package com.apollographql.ijplugin.telemetry

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager

data class Dependency(
    val group: String,
    val artifact: String,
    val version: String,
)

fun Project.getProjectDependencies(): Set<Dependency> {
  val dependencies = mutableSetOf<Dependency>()
  service<ProjectRootManager>().orderEntries().librariesOnly().forEachLibrary { library ->
    val components = library.name?.substringAfter(" ")?.split(":") ?: return@forEachLibrary true
    if (components.size < 3) return@forEachLibrary true
    dependencies.add(Dependency(components[0], components[1], components[2].substringBeforeLast("@")))
    true
  }
  return dependencies
}

fun Dependency.toTelemetryProperty(): TelemetryProperty? = when {
  group == "com.apollographql.apollo" || group == "com.apollographql.apollo3" -> TelemetryProperty.Dependency(group, artifact, version)
  group == "org.jetbrains.kotlin" && artifact == "kotlin-stdlib" -> TelemetryProperty.KotlinVersion(version)
  group == "androidx.compose.runtime" && artifact == "runtime" -> TelemetryProperty.ComposeVersion(version)
  else -> null
}

fun Set<Dependency>.toTelemetryProperties(): Set<TelemetryProperty> = mapNotNull { it.toTelemetryProperty() }.toSet()
