package com.apollographql.apollo

import android.util.Log
import com.apollographql.apollo.Logger

/**
 * This is an Android wrapper around [Logger] that will take any messages Apollo wants
 * to print and log them in the Android logcat.
 */
class ApolloAndroidLogger : Logger {

  override fun log(priority: Int, message: String, t: Throwable?, vararg args: Any) {
    val formattedMessage = message.format(*args)
    val tag = ApolloAndroidLogger::class.java.simpleName

    when (priority) {
      Logger.DEBUG -> Log.d(tag, formattedMessage)
      Logger.WARN -> Log.w(tag, formattedMessage)
      Logger.ERROR -> Log.e(tag, formattedMessage, t)
      else -> Log.d(tag, formattedMessage)
    }
  }
}
