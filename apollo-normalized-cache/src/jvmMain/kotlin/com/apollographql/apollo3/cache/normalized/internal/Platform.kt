package com.apollographql.apollo3.cache.normalized.internal

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal actual object Platform {
  actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
  }
}

actual typealias ReentrantReadWriteLock = ReentrantReadWriteLock

internal actual inline fun <T> ReentrantReadWriteLock.read(action: () -> T): T {
  return this.read(action)
}

internal actual inline fun <T> ReentrantReadWriteLock.write(action: () -> T): T {
  return this.write(action)
}
