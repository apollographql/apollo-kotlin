package com.apollographql.apollo3.mpp

actual fun currentTimeMillis(): Long {
  return System.currentTimeMillis()
}

actual fun currentThreadId(): String {
  return Thread.currentThread().id.toString()
}

actual fun ensureNeverFrozen(obj: Any) {
}

actual fun isFrozen(obj: Any) = false
actual fun freeze(obj: Any) {
}

actual fun assertMainThreadOnNative() {
}

actual fun platform() = Platform.Jvm