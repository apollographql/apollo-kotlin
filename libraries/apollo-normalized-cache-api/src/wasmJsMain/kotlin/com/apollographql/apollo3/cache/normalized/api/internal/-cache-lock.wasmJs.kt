package com.apollographql.apollo.cache.normalized.api.internal

internal actual class CacheLock actual constructor() {
  actual fun <T> lock(block: () -> T): T {
    return block()
  }
}