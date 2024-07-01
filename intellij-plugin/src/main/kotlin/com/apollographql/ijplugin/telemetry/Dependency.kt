package com.apollographql.ijplugin.telemetry

import com.apollographql.ijplugin.util.MavenCoordinates

fun MavenCoordinates.toTelemetryProperty(): TelemetryProperty? = when {
  group == "com.apollographql.apollo" || group == "com.apollographql.apollo" -> TelemetryProperty.Dependency(group, artifact, version)
  group == "org.jetbrains.kotlin" && artifact == "kotlin-stdlib" -> TelemetryProperty.KotlinVersion(version)
  group == "androidx.compose.runtime" && artifact == "runtime" -> TelemetryProperty.ComposeVersion(version)
  else -> null
}

fun Set<MavenCoordinates>.toTelemetryProperties(): Set<TelemetryProperty> = mapNotNull { it.toTelemetryProperty() }.toSet()
