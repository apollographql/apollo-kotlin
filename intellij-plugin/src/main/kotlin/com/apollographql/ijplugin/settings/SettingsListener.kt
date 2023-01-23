package com.apollographql.ijplugin.settings

import com.intellij.util.messages.Topic

interface SettingsListener {
  companion object {
    @Topic.ProjectLevel
    val TOPIC: Topic<SettingsListener> = Topic.create("Apollo GraphQL Settings", SettingsListener::class.java)
  }

  fun settingsChanged(settingsState: SettingsState)
}
