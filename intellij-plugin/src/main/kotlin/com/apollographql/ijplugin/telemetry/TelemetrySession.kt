package com.apollographql.ijplugin.telemetry

import java.time.Instant

sealed class TelemetryAttribute(
    val type: String,
    val parameters: Any?,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as TelemetryAttribute
    return type == other.type
  }

  override fun hashCode(): Int {
    return type.hashCode()
  }

  override fun toString(): String {
    return "TelemetryAttribute(type='$type', parameters=$parameters)"
  }

  /**
   * Gradle dependencies used by the project.
   */
  class Dependency(
      group: String,
      artifact: String,
      version: String,
  ) : TelemetryAttribute("dependency_$group:$artifact", mapOf("version" to version))

  /**
   * The version of Kotlin (per kotlin-stdlib).
   */
  class KotlinVersion(version: String) : TelemetryAttribute("kotlin_version", version)

  /**
   * The version of Compose (per androidx.compose.runtime).
   */
  class ComposeVersion(version: String) : TelemetryAttribute("compose_version", version)

  /**
   * The version of Gradle.
   */
  class GradleVersion(version: String) : TelemetryAttribute("gradle_version", version)

  /**
   * Android minSdk value.
   */
  class AndroidMinSdk(version: Int) : TelemetryAttribute("android_min_sdk", version)

  /**
   * Android targetSdk value.
   */
  class AndroidTargetSdk(version: Int) : TelemetryAttribute("android_target_sdk", version)

  /**
   * Android compileSdk value.
   */
  class AndroidCompileSdk(version: Int) : TelemetryAttribute("android_compile_sdk", version)

}

sealed class TelemetryEvent(
    val type: String,
    val parameters: Any?,
) {
  val date: Instant = Instant.now()

  override fun toString(): String {
    return "TelemetryEvent(date=$date, type='$type', parameters=$parameters)"
  }

  // TODO
  class ExampleEvent(parameters: Any?) : TelemetryEvent("example", parameters)
}

class TelemetryEventList {
  private val _events: MutableList<TelemetryEvent> = mutableListOf()
  val events: List<TelemetryEvent> = _events

  fun addEvent(telemetryEvent: TelemetryEvent) {
    _events.add(telemetryEvent)
  }

  fun clear() {
    _events.clear()
  }
}

data class TelemetrySession(
    val instanceId: String,
    val attributes: Set<TelemetryAttribute>,
    val events: List<TelemetryEvent>,
)
