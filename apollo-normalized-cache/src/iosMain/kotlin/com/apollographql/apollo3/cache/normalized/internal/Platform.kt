package com.apollographql.apollo.cache.normalized.internal

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.cinterop.convert
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_time

internal actual object Platform {
  actual fun currentTimeMillis(): Long {
    val nanoseconds: Long = dispatch_time(DISPATCH_TIME_NOW, 0).convert()
    return nanoseconds * 1_000_000L
  }
}

actual class ReentrantReadWriteLock {
  internal val lock = reentrantLock()
}

internal actual inline fun <T> ReentrantReadWriteLock.read(action: () -> T): T {
  return lock.withLock(action)
}

internal actual inline fun <T> ReentrantReadWriteLock.write(action: () -> T): T {
  return lock.withLock(action)
}

