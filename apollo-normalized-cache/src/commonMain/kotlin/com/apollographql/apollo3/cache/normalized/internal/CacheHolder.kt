package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache

class CacheHolder(producer: () -> OptimisticCache) {
  private val optimisticCache = producer()
  private val lock = ReentrantReadWriteLock()

  fun <T> read(block: (ReadOnlyNormalizedCache) -> T): T {
    return lock.read {
      block(optimisticCache)
    }
  }

  fun <T> write(block: (OptimisticCache) -> T): T {
    return lock.write {
      block(optimisticCache)
    }
  }

  fun writeAndForget(block: (OptimisticCache) -> Unit) {
    lock.write {
      block(optimisticCache)
    }
  }

}