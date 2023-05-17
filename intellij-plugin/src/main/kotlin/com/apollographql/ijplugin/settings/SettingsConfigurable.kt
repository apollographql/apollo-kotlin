package com.apollographql.ijplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SettingsConfigurable(private val project: Project) : Configurable {
  private var settingsComponent: SettingsComponent? = null

  override fun getDisplayName() = "Apollo GraphQL"

  override fun createComponent(): JComponent {
    val settingsComponent = SettingsComponent()
    this.settingsComponent = settingsComponent
    return settingsComponent.panel
  }

  override fun isModified(): Boolean {
    return settingsComponent!!.automaticCodegenTriggering != project.settingsState.automaticCodegenTriggering ||
        settingsComponent!!.contributeConfigurationToGraphqlPlugin != project.settingsState.contributeConfigurationToGraphqlPlugin
  }

  override fun apply() {
    project.settingsState.automaticCodegenTriggering = settingsComponent!!.automaticCodegenTriggering
    project.settingsState.contributeConfigurationToGraphqlPlugin = settingsComponent!!.contributeConfigurationToGraphqlPlugin
  }

  override fun reset() {
    settingsComponent!!.automaticCodegenTriggering = project.settingsState.automaticCodegenTriggering
    settingsComponent!!.contributeConfigurationToGraphqlPlugin = project.settingsState.contributeConfigurationToGraphqlPlugin
  }

  override fun getPreferredFocusedComponent() = settingsComponent!!.preferredFocusedComponent

  override fun disposeUIResources() {
    settingsComponent = null
  }
}
