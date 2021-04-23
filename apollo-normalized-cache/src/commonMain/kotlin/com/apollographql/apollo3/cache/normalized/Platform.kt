package com.apollographql.apollo3.cache.normalized

expect object Platform {
  fun currentTimeMillis(): Long
  fun currentThreadId(): String
  fun ensureNeverFrozen(obj: Any)
}