package com.apollographql.ijplugin.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager

fun runWriteActionInEdt(action: () -> Unit) {
  ApplicationManager.getApplication().invokeLater {
    ApplicationManager.getApplication().runWriteAction<Unit>(action)
  }
}

fun isProcessCanceled(): Boolean {
  try {
    ProgressManager.checkCanceled()
  } catch (e: ProcessCanceledException) {
    return true
  }
  return false
}
