package com.apollographql.ijplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SettingsConfigurable(private val project: Project) : Configurable {
  private lateinit var settingsComponent: SettingsComponent

  override fun createComponent(): JComponent {
    settingsComponent = SettingsComponent()
    return settingsComponent.panel
  }

  override fun getDisplayName(): String {
    return "Apollo GraphQL"
  }

  override fun isModified(): Boolean {
    return settingsComponent.automaticCodegenTriggering != project.settingsState.automaticCodegenTriggering
  }

  override fun apply() {
    project.settingsState.automaticCodegenTriggering = settingsComponent.automaticCodegenTriggering
  }

  override fun reset() {
    settingsComponent.automaticCodegenTriggering = project.settingsState.automaticCodegenTriggering
  }

  override fun getPreferredFocusedComponent() = settingsComponent.preferredFocusedComponent

  override fun disposeUIResources() {
    settingsComponent
  }
}
