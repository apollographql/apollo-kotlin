package com.apollographql.ijplugin.util

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.application
import java.util.concurrent.Callable
import java.util.concurrent.Future

fun runWriteActionInEdt(action: () -> Unit) {
  runInEdt {
    runWriteAction { action() }
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

inline fun <T> executeOnPooledThread(crossinline action: () -> T): Future<T> =
  application.executeOnPooledThread(Callable { action() })
