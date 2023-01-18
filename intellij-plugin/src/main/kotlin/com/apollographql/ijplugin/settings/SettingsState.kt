package com.apollographql.ijplugin.settings;

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.apollographql.ijplugin.settings.SettingsState",
    storages = [Storage("apollo.xml")]
)
class SettingsState : PersistentStateComponent<SettingsState> {
  var automaticCodegenTriggering: Boolean = true

  override fun getState(): SettingsState {
    return this
  }

  override fun loadState(state: SettingsState) {
    XmlSerializerUtil.copyBean(state, this)
  }

}

val Project.settingsState get() = service<SettingsState>()
