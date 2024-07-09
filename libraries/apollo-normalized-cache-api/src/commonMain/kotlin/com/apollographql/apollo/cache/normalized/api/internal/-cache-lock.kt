package com.apollographql.apollo.cache.normalized.api.internal

internal expect class CacheLock() {
  fun <T> lock(block: () -> T): T
}