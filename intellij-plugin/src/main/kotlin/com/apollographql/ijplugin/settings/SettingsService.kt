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
class SettingsService(private val project: Project) : PersistentStateComponent<SettingsStateImpl>, SettingsState {
  private var _state = SettingsStateImpl()

  override fun getState(): SettingsStateImpl {
    return _state
  }

  override fun loadState(state: SettingsStateImpl) {
    XmlSerializerUtil.copyBean(state, this._state)
    notifySettingsChanged()
  }

  override var automaticCodegenTriggering: Boolean
    get() = _state.automaticCodegenTriggering
    set(value) {
      _state.automaticCodegenTriggering = value
      notifySettingsChanged()
    }

  private fun notifySettingsChanged() {
    project.messageBus.syncPublisher(SettingsListener.TOPIC).settingsChanged(_state)
  }
}

interface SettingsState {
  var automaticCodegenTriggering: Boolean
}

class SettingsStateImpl : SettingsState {
  override var automaticCodegenTriggering: Boolean = true
}

val Project.settingsState get(): SettingsState = service<SettingsService>()
