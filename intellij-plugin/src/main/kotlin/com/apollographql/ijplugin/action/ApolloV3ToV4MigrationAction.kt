package com.apollographql.ijplugin.action

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.project.ApolloProjectService.ApolloVersion
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.refactoring.migration.v3tov4.ApolloV3ToV4MigrationProcessor
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ApolloV3ToV4MigrationAction : AnAction() {
  companion object {
    val ACTION_ID: String = ApolloV3ToV4MigrationAction::class.java.simpleName
  }

  override fun actionPerformed(e: AnActionEvent) {
    logd()
    e.project?.telemetryService?.logEvent(TelemetryEvent.ApolloIjMigrateToApollo4())
    val okCancelResult = Messages.showOkCancelDialog(
        e.project,
        ApolloBundle.message("action.ApolloV3ToV4MigrationAction.confirmDialog.message"),
        ApolloBundle.message("action.ApolloV3ToV4MigrationAction.confirmDialog.title"),
        ApolloBundle.message("action.MigrationAction.confirmDialog.ok"),
        ApolloBundle.message("action.MigrationAction.confirmDialog.cancel"),
        Messages.getQuestionIcon()
    )

    if (okCancelResult == Messages.OK) {
      ApolloV3ToV4MigrationProcessor(e.project ?: return).run()
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project?.apolloProjectService?.apolloVersion == ApolloVersion.V3
    e.presentation.isVisible = !ActionPlaces.isPopupPlace(e.place)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
