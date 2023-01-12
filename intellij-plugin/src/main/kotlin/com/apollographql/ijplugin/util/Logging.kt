package com.apollographql.ijplugin.util

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.utils.PrintingLogger

// To see debug logs, in the embedded IntelliJ app, go to Help / Diagnostic Tools / Debug Log Settings and add "Apollo"
// or pass -Didea.log.debug.categories=Apollo to the VM.
// See https://plugins.jetbrains.com/docs/intellij/ide-infrastructure.html#logging
// and https://plugins.jetbrains.com/docs/intellij/testing-faq.html#how-to-enable-debugtrace-logging
private val logger = if (isUnitTestMode()) PrintingLogger(System.err) else Logger.getInstance("Apollo")

fun logd() {
  logger.debug(prefix(null))
}

fun logd(message: String?) {
  logger.debug(prefix(message))
}

fun logd(any: Any?) {
  logger.debug(prefix(any?.toString()))
}

fun logd(throwable: Throwable) {
  logger.debug(throwable)
}

fun logd(throwable: Throwable, message: String) {
  logger.debug(prefix(message), throwable)
}

fun logd(throwable: Throwable, any: Any) {
  logger.debug(prefix(any.toString()), throwable)
}

fun logw(message: String) {
  logger.warn(message)
}

fun logw(any: Any) {
  logger.warn(any.toString())
}

fun logw(throwable: Throwable) {
  logger.warn(throwable)
}

fun logw(throwable: Throwable, message: String) {
  logger.warn(message, throwable)
}

fun logw(throwable: Throwable, any: Any) {
  logger.warn(any.toString(), throwable)
}

private fun getClassAndMethodName(): String {
  val stackTrace = Thread.currentThread().stackTrace
  val className = stackTrace[3].className.substringAfterLast('.')
  return className + " " + stackTrace[3].methodName
}

@Suppress("NOTHING_TO_INLINE")
private inline fun prefix(message: String?): String {
  return if (message == null) {
    getClassAndMethodName()
  } else {
    "${getClassAndMethodName()} - $message"
  }
}
