package com.apollographql.ijplugin.telemetry

import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.AndroidCompileSdk
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.AndroidGradlePluginVersion
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.AndroidMinSdk
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.AndroidTargetSdk
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.GradleVersion
import com.apollographql.ijplugin.util.logd
import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener

@Service(Service.Level.PROJECT)
class TelemetryService(
    private val project: Project,
) : Disposable {

  var gradleToolingModels: Set<ApolloGradleToolingModel> = emptySet()

  private val telemetryEventList: TelemetryEventList = TelemetryEventList()

  private var projectLibraries: Set<Dependency> = emptySet()

  init {
    logd("project=${project.name}")
    onLibrariesChanged()
    startObserveLibraries()
  }

  private fun startObserveLibraries() {
    logd()
    project.messageBus.connect(this).subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
      override fun rootsChanged(event: ModuleRootEvent) {
        logd("event=$event")
        onLibrariesChanged()
      }
    })
  }

  private fun onLibrariesChanged() {
    logd()
    projectLibraries = project.getProjectDependencies()
  }

  fun addEvent(telemetryEvent: TelemetryEvent) {
    telemetryEventList.addEvent(telemetryEvent)
  }

  private fun buildTelemetrySession(): TelemetrySession {
    return TelemetrySession(
        instanceId = "TODO", // TODO
        attributes = projectLibraries.toTelemetryAttributes() + gradleToolingModels.flatMap { it.toTelemetryAttributes() }.toSet(),
        events = telemetryEventList.events,
    )
  }

  override fun dispose() {
    logd("project=${project.name}")
  }

  fun sendTelemetry() {
    // TODO
    logd("buildTelemetrySession()=${buildTelemetrySession()}")
  }
}

val Project.telemetryService get() = service<TelemetryService>()

private fun ApolloGradleToolingModel.toTelemetryAttributes(): Set<TelemetryAttribute> = buildSet {
  // telemetryData was introduced in 1.2, accessing it on an older version will throw an exception
  if (versionMajor == 1 && versionMinor < 2) return@buildSet
  with(telemetryData) {
    gradleVersion?.let { add(GradleVersion(it)) }
    androidMinSdk?.let { add(AndroidMinSdk(it)) }
    androidTargetSdk?.let { add(AndroidTargetSdk(it)) }
    androidCompileSdk?.let { add(AndroidCompileSdk(it)) }
    androidAgpVersion?.let { add(AndroidGradlePluginVersion(it)) }
  }
}
