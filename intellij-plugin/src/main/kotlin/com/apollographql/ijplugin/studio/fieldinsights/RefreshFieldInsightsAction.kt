package com.apollographql.ijplugin.studio.fieldinsights

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.settings.SettingsConfigurable
import com.apollographql.ijplugin.settings.settingsState
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.Messages

class RefreshFieldInsightsAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    logd()
    val project = e.project ?: return

    if (project.settingsState.apolloKotlinServiceConfigurations.isEmpty()) {
      val okCancelResult = Messages.showOkCancelDialog(
          e.project,
          ApolloBundle.message("action.RefreshFieldInsightsAction.mustConfigureDialog.message"),
          ApolloBundle.message("action.RefreshFieldInsightsAction.mustConfigureDialog.title"),
          ApolloBundle.message("action.RefreshFieldInsightsAction.mustConfigureDialog.ok"),
          ApolloBundle.message("action.RefreshFieldInsightsAction.mustConfigureDialog.cancel"),
          Messages.getInformationIcon()
      )

      if (okCancelResult == Messages.OK) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsConfigurable::class.java)
      }
      return
    }

    val fieldInsightsService = project.service<FieldInsightsService>()
    fieldInsightsService.fetchLatencies()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
