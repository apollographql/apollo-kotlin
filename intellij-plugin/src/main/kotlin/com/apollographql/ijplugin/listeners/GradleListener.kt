package com.apollographql.ijplugin.listeners

import com.apollographql.ijplugin.services.apolloProjectService
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID

class GradleListener : ExternalSystemTaskNotificationListenerAdapter() {
  override fun onSuccess(id: ExternalSystemTaskId) {
    logd()
    if (id.projectSystemId == GRADLE_SYSTEM_ID && id.type == ExternalSystemTaskType.RESOLVE_PROJECT) {
      // Gradle sync finished, notify project service
      id.findProject()?.apolloProjectService()?.notifyGradleHasSynced()
    }
  }
}
