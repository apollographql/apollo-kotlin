package com.apollographql.ijplugin.settings.lsp

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.settings.appSettingsState
import com.apollographql.ijplugin.settings.projectSettingsState
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
    return settingsComponent!!.lspModeEnabled != appSettingsState.lspModeEnabled ||
        settingsComponent!!.passPathToSuperGraphYaml != project.projectSettingsState.lspPassPathToSuperGraphYaml ||
        settingsComponent!!.pathToSuperGraphYaml != project.projectSettingsState.lspPathToSuperGraphYaml ||
        settingsComponent!!.passAdditionalArguments != project.projectSettingsState.lspPassAdditionalArguments ||
        settingsComponent!!.additionalArguments != project.projectSettingsState.lspAdditionalArguments
  }

  override fun apply() {
    appSettingsState.lspModeEnabled = settingsComponent!!.lspModeEnabled
    project.projectSettingsState.lspPassPathToSuperGraphYaml = settingsComponent!!.passPathToSuperGraphYaml
    project.projectSettingsState.lspPathToSuperGraphYaml = settingsComponent!!.pathToSuperGraphYaml
    project.projectSettingsState.lspPassAdditionalArguments = settingsComponent!!.passAdditionalArguments
    project.projectSettingsState.lspAdditionalArguments = settingsComponent!!.additionalArguments
  }

  override fun reset() {
    settingsComponent!!.lspModeEnabled = appSettingsState.lspModeEnabled
    settingsComponent!!.passPathToSuperGraphYaml = project.projectSettingsState.lspPassPathToSuperGraphYaml
    settingsComponent!!.pathToSuperGraphYaml = project.projectSettingsState.lspPathToSuperGraphYaml
    settingsComponent!!.passAdditionalArguments = project.projectSettingsState.lspPassAdditionalArguments
    settingsComponent!!.additionalArguments = project.projectSettingsState.lspAdditionalArguments
  }

  override fun disposeUIResources() {
    settingsComponent = null
  }
}
