package com.apollographql.ijplugin.settings

import com.intellij.util.messages.Topic

interface ProjectSettingsListener {
  companion object {
    @Topic.ProjectLevel
    val TOPIC: Topic<ProjectSettingsListener> = Topic.create("Apollo GraphQL project settings", ProjectSettingsListener::class.java)
  }

  fun settingsChanged(projectSettingsState: ProjectSettingsState)
}
