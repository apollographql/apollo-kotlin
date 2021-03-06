package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache

expect class DefaultCacheHolder(producer: () -> OptimisticCache) {
  suspend fun <T> read(block: (ReadOnlyNormalizedCache) -> T): T

  suspend fun <T> write(block: (OptimisticCache) -> T): T

  fun writeAndForget(block: (OptimisticCache) -> Unit)
}