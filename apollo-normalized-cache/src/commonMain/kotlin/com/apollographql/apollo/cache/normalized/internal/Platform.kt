package com.apollographql.apollo.cache.normalized.internal

expect object Platform {
  fun currentTimeMillis(): Long
}
