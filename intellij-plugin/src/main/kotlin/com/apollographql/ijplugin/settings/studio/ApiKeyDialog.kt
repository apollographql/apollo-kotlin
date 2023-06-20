package com.apollographql.ijplugin.settings.studio

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.gradle.GradleToolingModelService
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.util.DialogValidationRequestors
import com.apollographql.ijplugin.util.validationOnApplyNotBlank
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion

class ApiKeyDialog(
    private val project: Project,
    var serviceName: String = "",
    var apiKey: String = "",
) : DialogWrapper(project, true) {
  private val isEdit = serviceName.isNotBlank()

  private val serviceCompletionProvider = object : TextFieldCompletionProvider(), DumbAware {
    override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
      result.addAllElements(getGraphqlProjectNames().map { LookupElementBuilder.create(it) })
    }
  }

  init {
    title = ApolloBundle.message(if (isEdit) "settings.studio.apiKeyDialog.title.edit" else "settings.studio.apiKeyDialog.title.add")
    if (!isEdit) {
      serviceName = getGraphqlProjectNames().firstOrNull() ?: ""
    }
    init()
  }

  override fun createCenterPanel(): DialogPanel = panel {
    row {
      val textFieldWithCompletion = TextFieldWithCompletion(project, serviceCompletionProvider, "", true, true, false, false)
      cell(textFieldWithCompletion)
          .label(ApolloBundle.message("settings.studio.apiKeyDialog.serviceName.label"), LabelPosition.TOP)
          .comment(ApolloBundle.message("settings.studio.apiKeyDialog.serviceName.comment"))
          .horizontalAlign(HorizontalAlign.FILL)
          .bind({ textField -> textField.text }, { textField, text -> textField.text = text }, ::serviceName.toMutableProperty())
          .validationRequestor(DialogValidationRequestors.WHEN_TEXT_FIELD_TEXT_CHANGED)
          .validationOnApplyNotBlank { text }
          .focused()

    }
    row {
      textField()
          .label(ApolloBundle.message("settings.studio.apiKeyDialog.apiKey.label"), LabelPosition.TOP)
          .horizontalAlign(HorizontalAlign.FILL)
          .bindText(::apiKey)
          .validationOnApplyNotBlank()
    }
  }.withPreferredWidth(350)

  private fun getGraphqlProjectNames(): List<String> {
    if (!project.apolloProjectService.isInitialized) return emptyList()
    return project.service<GradleToolingModelService>().graphQLProjectFiles.map { it.name }
  }
}
