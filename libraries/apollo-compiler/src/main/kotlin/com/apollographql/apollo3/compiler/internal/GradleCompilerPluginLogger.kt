package com.apollographql.apollo3.compiler.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.compiler.ApolloCompilerPluginLogger

@ApolloInternal
class GradleCompilerPluginLogger(val loglevel: Int) : ApolloCompilerPluginLogger {
  private val messager = System.out

  override fun logging(message: String) {
    if (loglevel <= LOGGING_LEVEL_LOGGING)
      messager.println("v: [apollo] $message")
  }

  override fun info(message: String) {
    if (loglevel <= LOGGING_LEVEL_INFO)
      messager.println("i: [apollo] $message")
  }

  override fun warn(message: String) {
    if (loglevel <= LOGGING_LEVEL_WARN)
      messager.println("w: [apollo] $message")
  }

  override fun error(message: String) {
    if (loglevel <= LOGGING_LEVEL_ERROR)
      messager.println("e: [apollo] $message")
  }

  override fun exception(e: Throwable) {
    if (loglevel <= LOGGING_LEVEL_ERROR)
      messager.println("e: [apollo] $e")
  }

  companion object {
    const val LOGGING_LEVEL_LOGGING = 0
    const val LOGGING_LEVEL_INFO = 1
    const val LOGGING_LEVEL_WARN = 3
    const val LOGGING_LEVEL_ERROR = 5
  }
}
