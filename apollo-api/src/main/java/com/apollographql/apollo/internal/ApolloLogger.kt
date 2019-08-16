package com.apollographql.apollo.internal

import com.apollographql.apollo.Logger
import com.apollographql.apollo.api.internal.Optional

class ApolloLogger(private val logger: Optional<Logger>) {

  fun d(message: String, vararg args: Any) = log(Logger.DEBUG, message, null, args)

  fun d(t: Throwable?, message: String, vararg args: Any) = log(Logger.DEBUG, message, t, args)

  fun w(message: String, vararg args: Any) = log(Logger.WARN, message, null, args)

  fun w(t: Throwable?, message: String, vararg args: Any) = log(Logger.WARN, message, t, args)

  fun e(message: String, vararg args: Any) = log(Logger.ERROR, message, null, args)

  fun e(t: Throwable?, message: String, vararg args: Any) = log(Logger.ERROR, message, t, args)

  private fun log(priority: Int, message: String, t: Throwable?, args: Array<out Any>) {
    if (logger.isPresent) {
      logger.get().log(priority, message, Optional.fromNullable(t), *args)
    }
  }
}
