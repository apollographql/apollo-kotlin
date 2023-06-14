package com.apollographql.ijplugin.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup

class ApolloToolsActionGroup : DefaultActionGroup() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
