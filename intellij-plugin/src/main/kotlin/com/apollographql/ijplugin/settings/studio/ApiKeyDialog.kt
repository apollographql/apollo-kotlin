package com.apollographql.ijplugin.settings.studio

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.gradle.GradleToolingModelService
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.settings.settingsState
import com.apollographql.ijplugin.util.validationOnApplyNotBlank
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.dsl.gridLayout.HorizontalAlign

class ApiKeyDialog(
    private val project: Project,
    var graphqlProjectName: String = "",
    var graphOsApiKey: String = "",
    var graphOsServiceName: String = "",
) : DialogWrapper(project, true) {
  private val isEdit = graphqlProjectName.isNotBlank()
  private lateinit var graphOsServiceNameTextField: JBTextField

  init {
    title = ApolloBundle.message(if (isEdit) "settings.studio.apiKeyDialog.title.edit" else "settings.studio.apiKeyDialog.title.add")
    if (!isEdit) {
      val alreadyConfiguredServiceNames = project.settingsState.serviceConfigurations.map { it.graphqlProjectName }
      graphqlProjectName = getGraphqlProjectNames().firstOrNull { it !in alreadyConfiguredServiceNames } ?: ""
    }
    init()
  }

  override fun createCenterPanel(): DialogPanel = panel {
    row {
      comboBox(getGraphqlProjectNames())
          .label(ApolloBundle.message("settings.studio.apiKeyDialog.graphqlProjectName.label"), LabelPosition.TOP)
          .horizontalAlign(HorizontalAlign.FILL)
          .bindItem(::graphqlProjectName.toNullableProperty())
          .focused()

    }
    row {
      textField()
          .label(ApolloBundle.message("settings.studio.apiKeyDialog.graphOsApiKey.label"), LabelPosition.TOP)
          .horizontalAlign(HorizontalAlign.FILL)
          .bindText(::graphOsApiKey)
          .validationOnApplyNotBlank()
          .applyToComponent {
            whenTextChanged {
              graphOsServiceNameTextField.text = it.document.getText(0, it.document.length).extractServiceName()
            }
          }
    }
    row {
      graphOsServiceNameTextField = textField()
          .label(ApolloBundle.message("settings.studio.apiKeyDialog.graphOsServiceName.label"), LabelPosition.TOP)
          .horizontalAlign(HorizontalAlign.FILL)
          .bindText(::graphOsServiceName)
          .validationOnApplyNotBlank()
          .component
    }
  }.withPreferredWidth(350)

  private fun getGraphqlProjectNames(): List<String> {
    if (!project.apolloProjectService.isInitialized) return emptyList()
    return project.service<GradleToolingModelService>().apolloKotlinServices.map { it.id.toString() }
  }

  private fun String.extractServiceName(): String? {
    // Service API keys are of the form: `service:<service name>:<key>`
    if (!startsWith("service:")) return null
    return split(":").getOrNull(1)
  }
}
