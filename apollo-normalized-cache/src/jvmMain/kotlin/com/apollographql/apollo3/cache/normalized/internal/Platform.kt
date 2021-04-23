package com.apollographql.apollo3.cache.normalized.internal

internal actual object Platform {
  actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
  }
}
