package com.apollographql.ijplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SettingsConfigurable(private val project: Project) : Configurable {
  private var settingsComponent: SettingsComponent? = null

  override fun getDisplayName() = "Apollo GraphQL"

  override fun createComponent(): JComponent {
    val settingsComponent = SettingsComponent(project)
    this.settingsComponent = settingsComponent
    return settingsComponent.panel
  }

  override fun isModified(): Boolean {
    return settingsComponent!!.automaticCodegenTriggering != project.projectSettingsState.automaticCodegenTriggering ||
        settingsComponent!!.contributeConfigurationToGraphqlPlugin != project.projectSettingsState.contributeConfigurationToGraphqlPlugin ||
        settingsComponent!!.apolloKotlinServiceConfigurations != project.projectSettingsState.apolloKotlinServiceConfigurations ||
        settingsComponent!!.telemetryEnabled != appSettingsState.telemetryEnabled
  }

  override fun apply() {
    project.projectSettingsState.automaticCodegenTriggering = settingsComponent!!.automaticCodegenTriggering
    project.projectSettingsState.contributeConfigurationToGraphqlPlugin = settingsComponent!!.contributeConfigurationToGraphqlPlugin
    project.projectSettingsState.apolloKotlinServiceConfigurations = settingsComponent!!.apolloKotlinServiceConfigurations
    appSettingsState.telemetryEnabled = settingsComponent!!.telemetryEnabled
  }

  override fun reset() {
    settingsComponent!!.automaticCodegenTriggering = project.projectSettingsState.automaticCodegenTriggering
    settingsComponent!!.contributeConfigurationToGraphqlPlugin = project.projectSettingsState.contributeConfigurationToGraphqlPlugin
    settingsComponent!!.apolloKotlinServiceConfigurations = project.projectSettingsState.apolloKotlinServiceConfigurations
    settingsComponent!!.telemetryEnabled = appSettingsState.telemetryEnabled
  }

  override fun getPreferredFocusedComponent() = settingsComponent!!.preferredFocusedComponent

  override fun disposeUIResources() {
    settingsComponent = null
  }
}
