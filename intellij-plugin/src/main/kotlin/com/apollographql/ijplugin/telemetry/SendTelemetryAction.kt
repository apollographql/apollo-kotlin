package com.apollographql.ijplugin.telemetry

import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Visible only in internal mode.
 */
class SendTelemetryAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    logd()
    val project = e.project ?: return

    project.telemetryService.sendTelemetry()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
