package com.apollographql.ijplugin.settings

import com.intellij.util.messages.Topic
import java.util.EventListener

interface SettingsListener : EventListener {
  fun settingsChanged(settingsState: SettingsState)

  companion object {
    val TOPIC: Topic<SettingsListener> = Topic.create("Apollo GraphQL Settings", SettingsListener::class.java)
  }
}
