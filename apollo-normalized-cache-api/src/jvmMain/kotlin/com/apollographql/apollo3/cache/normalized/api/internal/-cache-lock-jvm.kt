package com.apollographql.apollo3.cache.normalized.api.internal

import java.util.concurrent.atomic.AtomicInteger

actual class CacheLock actual constructor() {
  actual fun <T> lock(block: () -> T): T {
    return synchronized(this) {
      block()
    }
  }
}