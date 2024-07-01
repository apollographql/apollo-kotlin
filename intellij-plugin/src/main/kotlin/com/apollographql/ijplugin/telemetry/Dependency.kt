package com.apollographql.ijplugin.telemetry

import com.apollographql.ijplugin.util.MavenCoordinates
import com.apollographql.ijplugin.util.apollo2
import com.apollographql.ijplugin.util.apollo3
import com.apollographql.ijplugin.util.apollo4

@Suppress("KotlinConstantConditions")
fun MavenCoordinates.toTelemetryProperty(): TelemetryProperty? = when {
  group == apollo2 || group == apollo3 || group == apollo4 -> TelemetryProperty.Dependency(group, artifact, version)
  group == "org.jetbrains.kotlin" && artifact == "kotlin-stdlib" -> TelemetryProperty.KotlinVersion(version)
  group == "androidx.compose.runtime" && artifact == "runtime" -> TelemetryProperty.ComposeVersion(version)
  else -> null
}

fun Set<MavenCoordinates>.toTelemetryProperties(): Set<TelemetryProperty> = mapNotNull { it.toTelemetryProperty() }.toSet()
