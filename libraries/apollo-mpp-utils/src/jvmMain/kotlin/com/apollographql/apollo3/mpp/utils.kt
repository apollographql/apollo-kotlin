package com.apollographql.apollo3.mpp

import java.text.SimpleDateFormat
import java.util.Locale

actual fun currentTimeMillis(): Long {
  return System.currentTimeMillis()
}

private val simpleDateFormat by lazy { SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT) }

actual fun currentTimeFormatted(): String {
  return simpleDateFormat.format(currentTimeMillis())
}

actual fun currentThreadId(): String {
  return Thread.currentThread().id.toString()
}

actual fun currentThreadName(): String {
  return Thread.currentThread().name
}

actual fun ensureNeverFrozen(obj: Any) {
}

actual fun isFrozen(obj: Any) = false
actual fun freeze(obj: Any) {
}

actual fun assertMainThreadOnNative() {
}

actual fun platform() = Platform.Jvm
