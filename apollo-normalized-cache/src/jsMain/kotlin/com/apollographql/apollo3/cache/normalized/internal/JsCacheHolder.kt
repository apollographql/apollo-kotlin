package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache

actual class DefaultCacheHolder actual constructor(producer: () -> OptimisticCache) {
  private val optimisticCache = producer()

  actual suspend fun <T> read(block: (ReadOnlyNormalizedCache) -> T): T {
    return block(optimisticCache)

  }

  actual suspend fun <T> write(block: (OptimisticCache) -> T): T {
    return block(optimisticCache)
  }

  actual fun writeAndForget(block: (OptimisticCache) -> Unit) {
      block(optimisticCache)
  }
}