package com.apollographql.apollo3

/**
 * Logger to use for logging by the [com.apollographql.apollo3.ApolloClient]
 */
interface Logger {
  /**
   * Logs the message to the appropriate channel (file, console, etc)
   *
   * @param priority the priority to set
   * @param message message to log
   * @param t Optional throwable to log
   * @param args extra arguments to pass to the logged message.
   */
  fun log(priority: Int, message: String, t: Throwable?, vararg args: Any)

  companion object {
    const val DEBUG = 3
    const val WARN = 5
    const val ERROR = 6
  }
}
