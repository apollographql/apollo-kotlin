package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache

expect class Guard<R: Any>(name: String, producer: () -> R) {
  fun <T> access(block: (R) -> T): T

  fun writeAndForget(block: (R) -> Unit)
}