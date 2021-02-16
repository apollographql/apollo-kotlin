package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.Logger
import com.apollographql.apollo3.Logger.Companion.DEBUG
import com.apollographql.apollo3.Logger.Companion.ERROR
import com.apollographql.apollo3.Logger.Companion.WARN

class ApolloLogger(val logger: Logger?) {

  fun d(message: String, vararg args: Any) =
      log(DEBUG, message, null, *args)

  fun d(t: Throwable?, message: String, vararg args: Any) =
      log(DEBUG, message, t, *args)

  fun w(message: String, vararg args: Any) =
      log(WARN, message, null, *args)

  fun w(t: Throwable?, message: String, vararg args: Any) =
      log(WARN, message, t, *args)

  fun e(message: String, vararg args: Any) =
      log(ERROR, message, null, *args)

  fun e(t: Throwable?, message: String, vararg args: Any) =
      log(ERROR, message, t, *args)

  private fun log(priority: Int, message: String, t: Throwable?, vararg args: Any) {
    logger?.log(priority, message, t, *args)
  }

}
