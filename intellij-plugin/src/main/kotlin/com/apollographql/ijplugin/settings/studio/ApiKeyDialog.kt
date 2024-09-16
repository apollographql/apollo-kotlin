package com.apollographql.ijplugin.settings.studio

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.apollographql.ijplugin.gradle.GradleToolingModelService
import com.apollographql.ijplugin.util.validationOnApplyNotBlank
import com.intellij.openapi.observable.util.whenItemSelectedFromUi
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import java.util.Vector
import javax.swing.DefaultComboBoxModel

class ApiKeyDialog(
    private val project: Project,
    apolloKotlinServiceId: ApolloKotlinService.Id? = null,
    var graphOsApiKey: String = "",
    var graphOsServiceName: String = "",
) : DialogWrapper(project, true) {
  private var gradleProjectName: String
  private var apolloKotlinServiceName: String

  private lateinit var gradleProjectNameComboBox: ComboBox<String>
  private lateinit var graphOsServiceNameTextField: JBTextField

  val apolloKotlinServiceId: ApolloKotlinService.Id
    get() = ApolloKotlinService.Id(gradleProjectName, apolloKotlinServiceName)

  init {
    val isEdit = apolloKotlinServiceId != null
    title = ApolloBundle.message(if (isEdit) "settings.studio.apiKeyDialog.title.edit" else "settings.studio.apiKeyDialog.title.add")
    gradleProjectName = if (isEdit) {
      apolloKotlinServiceId!!.gradleProjectPath
    } else {
      getGradleProjectNames().firstOrNull() ?: ""
    }
    apolloKotlinServiceName = if (isEdit) {
      apolloKotlinServiceId!!.serviceName
    } else {
      getApolloKotlinServiceNames(gradleProjectName).firstOrNull() ?: ""
    }
    init()
  }

  override fun createCenterPanel(): DialogPanel = panel {
    row {
      comboBox(getGradleProjectNames())
          .label(ApolloBundle.message("settings.studio.apiKeyDialog.gradleProjectName.label"), LabelPosition.TOP)
          .align(AlignX.FILL)
          .bindItem(::gradleProjectName.toNullableProperty())
          .focused()
          .applyToComponent {
            @Suppress("UnstableApiUsage")
            whenItemSelectedFromUi {
              gradleProjectNameComboBox.model = DefaultComboBoxModel(Vector(getApolloKotlinServiceNames(it)))
            }
          }
    }
    row {
      gradleProjectNameComboBox = comboBox(getApolloKotlinServiceNames(gradleProjectName))
          .label(ApolloBundle.message("settings.studio.apiKeyDialog.apolloKotlinServiceName.label"), LabelPosition.TOP)
          .align(AlignX.FILL)
          .bindItem(::apolloKotlinServiceName.toNullableProperty())
          .component
    }
    row {
      textField()
          .label(ApolloBundle.message("settings.studio.apiKeyDialog.graphOsApiKey.label"), LabelPosition.TOP)
          .applyToComponent {
            emptyText.text = ApolloBundle.message("settings.studio.apiKeyDialog.graphOsApiKey.emptyText")
          }
          .align(AlignX.FILL)
          .bindText(::graphOsApiKey)
          .validationOnApplyNotBlank()
          .validationOnApply { component ->
            if (!component.text.matches(Regex("(service:.+:.+)|(user:.+:.+)"))) {
              ValidationInfo(ApolloBundle.message("settings.studio.apiKeyDialog.graphOsApiKey.invalid"), component)
            } else {
              null
            }
          }
          .applyToComponent {
            whenTextChanged {
              graphOsServiceNameTextField.text = it.document.getText(0, it.document.length).extractServiceName()
            }
          }
    }
    row {
      graphOsServiceNameTextField = textField()
          .label(ApolloBundle.message("settings.studio.apiKeyDialog.graphOsGraphName.label"), LabelPosition.TOP)
          .align(AlignX.FILL)
          .bindText(::graphOsServiceName)
          .validationOnApplyNotBlank()
          .component
    }
  }.withPreferredWidth(450)

  private fun getGradleProjectNames(): List<String> {
    return GradleToolingModelService.getApolloKotlinServices(project).map { it.id.gradleProjectPath }.distinct().sorted()
  }

  private fun getApolloKotlinServiceNames(gradleProjectName: String): List<String> {
    return GradleToolingModelService.getApolloKotlinServices(project)
        .filter { it.id.gradleProjectPath == gradleProjectName }
        .map { it.id.serviceName }
        .sorted()
  }

  private fun String.extractServiceName(): String? {
    // Service API keys are of the form: `service:<service name>:<key>`
    if (!startsWith("service:")) return null
    return split(":").getOrNull(1)
  }
}
