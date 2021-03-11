package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache
import java.util.concurrent.locks.ReentrantReadWriteLock

actual class DefaultCacheHolder actual constructor(producer: () -> OptimisticCache) {
  private val lock = ReentrantReadWriteLock()
  private val optimisticCache = producer()

  actual suspend fun <T> read(block: (ReadOnlyNormalizedCache) -> T): T {
    return lock.read {
      block(optimisticCache)
    }
  }

  actual suspend fun <T> write(block: (OptimisticCache) -> T): T {
    return lock.write {
      block(optimisticCache)
    }
  }

  actual fun writeAndForget(block: (OptimisticCache) -> Unit) {
    lock.write {
      block(optimisticCache)
    }
  }
}