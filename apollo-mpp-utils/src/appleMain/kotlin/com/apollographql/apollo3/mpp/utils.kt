package com.apollographql.apollo3.mpp

import kotlinx.cinterop.convert
import platform.Foundation.NSThread
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_time
import platform.posix.pthread_self

actual fun currentTimeMillis(): Long {
  val nanoseconds: Long = dispatch_time(DISPATCH_TIME_NOW, 0).convert()
  return nanoseconds / 1_000_000L
}
actual fun currentThreadId(): String {
  return pthread_self()?.rawValue.toString()
}

actual fun assertMainThreadOnNative() {
  check(NSThread.isMainThread()) {
    "Non-main native call"
  }
}

actual fun platform() = Platform.Native
