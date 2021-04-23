package com.apollographql.apollo3.cache.normalized

import kotlinx.cinterop.convert
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_time
import platform.posix.pthread_self
import kotlin.native.concurrent.ensureNeverFrozen

actual object Platform {
  actual fun currentTimeMillis(): Long {
    val nanoseconds: Long = dispatch_time(DISPATCH_TIME_NOW, 0).convert()
    return nanoseconds * 1_000_000L
  }
  actual fun currentThreadId(): String {
    return pthread_self()?.toString() ?: "?"
  }
  actual fun ensureNeverFrozen(obj: Any) {
    obj.ensureNeverFrozen()
  }
}