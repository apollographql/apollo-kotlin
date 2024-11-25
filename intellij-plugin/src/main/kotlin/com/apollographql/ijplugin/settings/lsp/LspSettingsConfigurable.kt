package com.apollographql.ijplugin.settings.lsp

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.settings.appSettingsState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class LspSettingsConfigurable(private val project: Project) : Configurable, Configurable.Beta {
  private var settingsComponent: LspSettingsComponent? = null

  override fun getDisplayName() = ApolloBundle.message("settings.rover.title")

  override fun createComponent(): JComponent {
    val settingsComponent = LspSettingsComponent(project)
    this.settingsComponent = settingsComponent
    return settingsComponent.panel
  }

  override fun isModified(): Boolean {
    return settingsComponent!!.lspModeEnabled != appSettingsState.lspModeEnabled
  }

  override fun apply() {
    appSettingsState.lspModeEnabled = settingsComponent!!.lspModeEnabled
  }

  override fun reset() {
    settingsComponent!!.lspModeEnabled = appSettingsState.lspModeEnabled
  }

  override fun disposeUIResources() {
    settingsComponent = null
  }
}
