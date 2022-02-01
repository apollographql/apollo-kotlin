package com.apollographql.apollo3.cache.normalized.api.internal

actual class CacheLock actual constructor() {
  actual fun <T> lock(block: () -> T): T = block()
}