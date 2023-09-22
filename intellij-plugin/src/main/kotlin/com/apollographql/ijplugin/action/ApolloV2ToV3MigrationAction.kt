package com.apollographql.ijplugin.action

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.project.ApolloProjectService.ApolloVersion
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.refactoring.migration.v2tov3.ApolloV2ToV3MigrationProcessor
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ApolloV2ToV3MigrationAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    logd()
    e.project?.telemetryService?.logEvent(TelemetryEvent.ApolloIjMigrateToApollo3())
    val okCancelResult = Messages.showOkCancelDialog(
        e.project,
        ApolloBundle.message("action.ApolloV2ToV3MigrationAction.confirmDialog.message"),
        ApolloBundle.message("action.ApolloV2ToV3MigrationAction.confirmDialog.title"),
        ApolloBundle.message("action.MigrationAction.confirmDialog.ok"),
        ApolloBundle.message("action.MigrationAction.confirmDialog.cancel"),
        Messages.getQuestionIcon()
    )

    if (okCancelResult == Messages.OK) {
      ApolloV2ToV3MigrationProcessor(e.project ?: return).run()
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project?.apolloProjectService?.apolloVersion == ApolloVersion.V2
    e.presentation.isVisible = !ActionPlaces.isPopupPlace(e.place)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
