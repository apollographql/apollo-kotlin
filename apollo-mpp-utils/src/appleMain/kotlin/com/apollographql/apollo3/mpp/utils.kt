package com.apollographql.apollo3.mpp

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970
import platform.posix.pthread_self
import kotlin.native.concurrent.ensureNeverFrozen
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen
import kotlin.system.getTimeMillis

actual fun currentTimeMillis(): Long {
  return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

private val nsDateFormatter by lazy { NSDateFormatter().apply { dateFormat = "HH:mm:ss.SSS" } }

actual fun currentTimeFormatted(): String {
  return nsDateFormatter.stringFromDate(NSDate())
}

actual fun currentThreadId(): String {
  return pthread_self()?.rawValue.toString()
}

actual fun currentThreadName(): String {
  return if (NSThread.isMainThread) {
    "main"
  } else {
    currentThreadId()
  }
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
