package com.apollographql.ijplugin.util

import com.intellij.openapi.application.ApplicationManager

fun runWriteActionInEdt(action: () -> Unit) {
  ApplicationManager.getApplication().invokeLater {
    ApplicationManager.getApplication().runWriteAction<Unit>(action)
  }
}
