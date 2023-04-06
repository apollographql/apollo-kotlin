package com.apollographql.ijplugin.action

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.refactoring.migration.compattooperationbased.CompatToOperationBasedCodegenMigrationProcessor
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class CompatToOperationBasedCodegenMigrationAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    logd()
    val okCancelResult = Messages.showOkCancelDialog(
        e.project,
        ApolloBundle.message("action.CompatToOperationBasedCodegenMigrationAction.confirmDialog.message"),
        ApolloBundle.message("action.CompatToOperationBasedCodegenMigrationAction.confirmDialog.title"),
        ApolloBundle.message("action.CompatToOperationBasedCodegenMigrationAction.confirmDialog.ok"),
        ApolloBundle.message("action.CompatToOperationBasedCodegenMigrationAction.confirmDialog.cancel"),
        Messages.getQuestionIcon()
    )

    if (okCancelResult == Messages.OK) {
      CompatToOperationBasedCodegenMigrationProcessor(e.project ?: return).run()
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project?.apolloProjectService?.isApolloKotlin3Project == true
    e.presentation.isVisible = !ActionPlaces.isPopupPlace(e.place)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
