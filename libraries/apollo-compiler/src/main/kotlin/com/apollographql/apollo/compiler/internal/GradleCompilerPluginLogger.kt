package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.compiler.ApolloCompilerPluginLogger

@ApolloInternal
class GradleCompilerPluginLogger(val loglevel: Int) : ApolloCompilerPluginLogger {

  override fun logging(message: String) {
    if (loglevel <= LOGGING_LEVEL_LOGGING)
      println("v: [apollo] $message")
  }

  override fun info(message: String) {
    if (loglevel <= LOGGING_LEVEL_INFO)
      println("i: [apollo] $message")
  }

  override fun warn(message: String) {
    if (loglevel <= LOGGING_LEVEL_WARN)
      println("w: [apollo] $message")
  }

  override fun error(message: String) {
    if (loglevel <= LOGGING_LEVEL_ERROR)
      println("e: [apollo] $message")
  }

  companion object {
    /**
     * Matches Gradle LogLevel
     * See https://github.com/gradle/gradle/blob/71f42531a742bc263c61f1d0dc21bb6570cc817b/platforms/core-runtime/logging-api/src/main/java/org/gradle/api/logging/LogLevel.java#L21
     */
    const val LOGGING_LEVEL_LOGGING = 0
    const val LOGGING_LEVEL_INFO = 1
    const val LOGGING_LEVEL_WARN = 3
    const val LOGGING_LEVEL_ERROR = 5
  }
}
