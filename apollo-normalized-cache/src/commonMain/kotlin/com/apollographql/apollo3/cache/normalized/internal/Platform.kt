package com.apollographql.apollo3.cache.normalized.internal

internal expect object Platform {
  fun currentTimeMillis(): Long
}
