package com.apollographql.ijplugin.error

import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.util.Date

/**
 * Visible only in internal mode.
 */
class ThrowAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    logd()
    throw RuntimeException("This is a test exception. ${Date()}")
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
