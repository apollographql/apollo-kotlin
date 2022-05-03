package com.apollographql.apollo3.mpp

import platform.Foundation.NSThread
import platform.posix.pthread_self
import kotlin.native.concurrent.ensureNeverFrozen
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen
import kotlin.system.getTimeMillis

actual fun currentTimeMillis(): Long {
  return getTimeMillis()
}

actual fun currentThreadId(): String {
  return pthread_self()?.rawValue.toString()
}

actual fun ensureNeverFrozen(obj: Any) {
  obj.ensureNeverFrozen()
}

actual fun isFrozen(obj: Any) = obj.isFrozen

actual fun freeze(obj: Any) {
  obj.freeze()
}

actual fun assertMainThreadOnNative() {
  check(NSThread.isMainThread()) {
    "Non-main native call"
  }
}

actual fun platform() = Platform.Native
