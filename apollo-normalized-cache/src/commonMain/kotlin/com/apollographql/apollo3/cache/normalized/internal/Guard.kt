package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache

expect class Guard<R: Any>(name: String, producer: () -> R) {
  suspend fun <T> access(block: (R) -> T): T

  fun <T> blockingAccess(block: (R) -> T): T

  fun writeAndForget(block: (R) -> Unit)

  fun dispose()
}