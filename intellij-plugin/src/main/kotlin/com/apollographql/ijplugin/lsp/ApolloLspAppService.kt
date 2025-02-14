package com.apollographql.ijplugin.lsp

import com.apollographql.ijplugin.settings.AppSettingsListener
import com.apollographql.ijplugin.settings.AppSettingsState
import com.apollographql.ijplugin.settings.ProjectSettingsListener
import com.apollographql.ijplugin.settings.ProjectSettingsState
import com.apollographql.ijplugin.settings.appSettingsState
import com.apollographql.ijplugin.settings.projectSettingsState
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.application

@Service(Service.Level.APP)
class ApolloLspAppService : Disposable {
  init {
    logd()
    startObserveSettings()
  }

  private fun startObserveSettings() {
    logd()
    application.messageBus.connect(this).subscribe(AppSettingsListener.TOPIC, object : AppSettingsListener {
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

@Service(Service.Level.PROJECT)
class ApolloLspProjectService(private val project: Project) : Disposable {
  init {
    logd()
    startObserveSettings()
    startObserveVfsChanges()
  }

  private fun startObserveSettings() {
    logd()
    project.messageBus.connect(this).subscribe(ProjectSettingsListener.TOPIC, object : ProjectSettingsListener {
      var lspPassPathToSuperGraphYaml = project.projectSettingsState.lspPassPathToSuperGraphYaml
      var lspPathToSuperGraphYaml = project.projectSettingsState.lspPathToSuperGraphYaml
      var lspPassAdditionalArguments = project.projectSettingsState.lspPassAdditionalArguments
      var lspAdditionalArguments = project.projectSettingsState.lspAdditionalArguments

      override fun settingsChanged(projectSettingsState: ProjectSettingsState) {
        val lspPassPathToSuperGraphYamlChanged = lspPassPathToSuperGraphYaml != projectSettingsState.lspPassPathToSuperGraphYaml
        val lspPathToSuperGraphYamlChanged = lspPathToSuperGraphYaml != projectSettingsState.lspPathToSuperGraphYaml
        val lspPassAdditionalArgumentsChanged = lspPassAdditionalArguments != projectSettingsState.lspPassAdditionalArguments
        val lspAdditionalArgumentsChanged = lspAdditionalArguments != projectSettingsState.lspAdditionalArguments
        lspPassPathToSuperGraphYaml = projectSettingsState.lspPassPathToSuperGraphYaml
        lspPathToSuperGraphYaml = projectSettingsState.lspPathToSuperGraphYaml
        lspPassAdditionalArguments = projectSettingsState.lspPassAdditionalArguments
        lspAdditionalArguments = projectSettingsState.lspAdditionalArguments
        logd("lspPassPathToSuperGraphYamlChanged=$lspPassPathToSuperGraphYamlChanged lspPathToSuperGraphYamlChanged=$lspPathToSuperGraphYamlChanged lspPassAdditionalArgumentsChanged=$lspPassAdditionalArgumentsChanged lspAdditionalArgumentsChanged=$lspAdditionalArgumentsChanged")
        if (lspPassPathToSuperGraphYamlChanged || lspPathToSuperGraphYamlChanged || lspPassAdditionalArgumentsChanged || lspAdditionalArgumentsChanged) {
          restartApolloLsp()
        }
      }
    })
  }

  private fun startObserveVfsChanges() {
    project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
      override fun after(events: MutableList<out VFileEvent>) {
        for (event in events) {
          val vFile = event.file!!
          val isSupergraphYaml = vFile == project.guessProjectDir()?.findChild("supergraph.yaml") ||
              vFile.path == project.projectSettingsState.lspPathToSuperGraphYaml
          if (isSupergraphYaml) {
            logd("supergraph.yaml changed: restarting Apollo LSP")
            restartApolloLsp()
          }
        }
      }
    })
  }

  override fun dispose() {
    logd()
  }
}
