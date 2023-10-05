package com.apollographql.ijplugin.settings

import com.intellij.util.messages.Topic

interface AppSettingsListener {
  companion object {
    @Topic.ProjectLevel
    val TOPIC: Topic<AppSettingsListener> = Topic.create("Apollo GraphQL app settings", AppSettingsListener::class.java)
  }

  fun settingsChanged(appSettingsState: AppSettingsState)
}
