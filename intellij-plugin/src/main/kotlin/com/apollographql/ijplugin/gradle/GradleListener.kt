package com.apollographql.ijplugin.gradle

import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID

class GradleListener : ExternalSystemTaskNotificationListener {
  override fun onSuccess(id: ExternalSystemTaskId) {
    logd()
    if (id.projectSystemId == GRADLE_SYSTEM_ID && id.type == ExternalSystemTaskType.RESOLVE_PROJECT) {
      // Gradle sync finished, notify interesting parties
      id.findProject()?.messageBus?.syncPublisher(GradleHasSyncedListener.TOPIC)?.gradleHasSynced()
    }
  }
}

interface GradleHasSyncedListener {
  companion object {
    @Topic.ProjectLevel
    val TOPIC: Topic<GradleHasSyncedListener> = Topic.create("Gradle has synced", GradleHasSyncedListener::class.java)
  }

  fun gradleHasSynced()
}

