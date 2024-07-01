package com.apollographql.apollo.cache.normalized.internal

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal actual class Lock {
  private val lock = ReentrantReadWriteLock()

  actual fun <T> read(block: () -> T): T {
    return lock.read {
      block()
    }
  }

  actual fun <T> write(block: () -> T): T {
    return lock.write {
      block()
    }
  }
}
