package com.apollographql.ijplugin.lsp

import com.apollographql.ijplugin.settings.AppSettingsListener
import com.apollographql.ijplugin.settings.AppSettingsState
import com.apollographql.ijplugin.settings.appSettingsState
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx

@Service(Service.Level.APP)
class ApolloLspService : Disposable {
  init {
    logd()
    startObserveSettings()
  }

  private fun startObserveSettings() {
    logd()
    ApplicationManager.getApplication().messageBus.connect(this).subscribe(AppSettingsListener.TOPIC, object : AppSettingsListener {
      var lspModeEnabled = appSettingsState.lspModeEnabled
      override fun settingsChanged(appSettingsState: AppSettingsState) {
        val lspModeEnabledChanged = lspModeEnabled != appSettingsState.lspModeEnabled
        lspModeEnabled = appSettingsState.lspModeEnabled
        logd("lspModeEnabledChanged=$lspModeEnabledChanged")
        if (lspModeEnabledChanged) {
          runWriteAction {
            FileTypeManagerEx.getInstanceEx().makeFileTypesChange("Apollo GraphQL file type change", {})
          }
          restartApolloLsp()
        }
      }
    })
  }

  override fun dispose() {
    logd()
  }
}
