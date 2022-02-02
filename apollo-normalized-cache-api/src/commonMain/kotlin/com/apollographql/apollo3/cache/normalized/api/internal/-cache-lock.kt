package com.apollographql.apollo3.cache.normalized.api.internal

internal expect class CacheLock() {
  fun <T> lock(block: () -> T): T
}