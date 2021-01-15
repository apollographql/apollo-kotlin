package com.apollographql.apollo.cache.normalized.internal

actual object Platform {
  actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
  }
}
