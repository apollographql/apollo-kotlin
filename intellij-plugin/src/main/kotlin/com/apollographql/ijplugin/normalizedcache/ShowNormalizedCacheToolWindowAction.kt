package com.apollographql.ijplugin.normalizedcache

import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.ToolWindowManager

class ShowNormalizedCacheToolWindowAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    logd()
    e.project?.let { ToolWindowManager.getInstance(it).getToolWindow("NormalizedCacheViewer") }?.show()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
