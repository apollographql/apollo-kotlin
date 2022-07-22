package com.apollographql.apollo3.mpp

import kotlin.js.Date

actual fun currentTimeMillis(): Long {
  return Date().getTime().toLong()
}

actual fun currentTimeFormatted(): String {
  return Date().toISOString()
}

actual fun currentThreadId(): String {
  return "js"
}

actual fun currentThreadName(): String {
  return currentThreadId()
}

actual fun ensureNeverFrozen(obj: Any) {
}

actual fun isFrozen(obj: Any) = false
actual fun freeze(obj: Any) {
}

actual fun assertMainThreadOnNative() {
}

actual fun platform() = Platform.Js
