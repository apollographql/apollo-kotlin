package com.apollographql.ijplugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.application
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application level settings.
 */
@Service(Service.Level.APP)
@State(
    name = "com.apollographql.ijplugin.settings.AppSettingsState",
    storages = [Storage("apollo.xml")]
)
class AppSettingsService : PersistentStateComponent<AppSettingsStateImpl>, AppSettingsState {
  private val _state = AppSettingsStateImpl()

  override fun getState(): AppSettingsStateImpl {
    return _state
  }

  override fun loadState(state: AppSettingsStateImpl) {
    XmlSerializerUtil.copyBean(state, this._state)
    notifySettingsChanged()
  }

  override var hasShownTelemetryOptOutDialog: Boolean
    get() = _state.hasShownTelemetryOptOutDialog
    set(value) {
      _state.hasShownTelemetryOptOutDialog = value
      notifySettingsChanged()
    }

  override var telemetryEnabled: Boolean
    get() = _state.telemetryEnabled
    set(value) {
      _state.telemetryEnabled = value
      notifySettingsChanged()
    }

  override var lspModeEnabled: Boolean
    get() = _state.lspModeEnabled
    set(value) {
      _state.lspModeEnabled = value
      notifySettingsChanged()
    }

  private var lastNotifiedState: AppSettingsState? = null
  private fun notifySettingsChanged() {
    if (lastNotifiedState != _state) {
      lastNotifiedState = _state.copy()
      application.messageBus.syncPublisher(AppSettingsListener.TOPIC).settingsChanged(_state)
    }
  }
}

interface AppSettingsState {
  var hasShownTelemetryOptOutDialog: Boolean
  var telemetryEnabled: Boolean
  var lspModeEnabled: Boolean
}

data class AppSettingsStateImpl(
    override var hasShownTelemetryOptOutDialog: Boolean = false,
    override var telemetryEnabled: Boolean = true,
    override var lspModeEnabled: Boolean = false,
) : AppSettingsState

val appSettingsState get(): AppSettingsState = service<AppSettingsService>()
