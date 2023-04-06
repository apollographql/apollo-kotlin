package com.apollographql.ijplugin.settings;

import com.intellij.lang.jsgraphql.GraphQLSettings
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

  override var hasEnabledGraphQLPluginApolloKotlinSupport: Boolean
    get() = _state.hasEnabledGraphQLPluginApolloKotlinSupport
    set(value) {
      _state.hasEnabledGraphQLPluginApolloKotlinSupport = value
    }

  private fun notifySettingsChanged() {
    project.messageBus.syncPublisher(SettingsListener.TOPIC).settingsChanged(_state)
  }

  init {
    // Automatically enable the "Frameworks / Apollo Kotlin" support in the GraphQL plugin's settings
    if (!hasEnabledGraphQLPluginApolloKotlinSupport) {
      project.service<GraphQLSettings>().setApolloKotlinSupportEnabled(true)
      hasEnabledGraphQLPluginApolloKotlinSupport = true
    }
  }
}

interface SettingsState {
  var automaticCodegenTriggering: Boolean
  var hasEnabledGraphQLPluginApolloKotlinSupport: Boolean
}

class SettingsStateImpl : SettingsState {
  override var automaticCodegenTriggering: Boolean = true
  override var hasEnabledGraphQLPluginApolloKotlinSupport: Boolean = false
}

val Project.settingsState get(): SettingsState = service<SettingsService>()
