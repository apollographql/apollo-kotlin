package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.cache.normalized.internal.RealApolloStore

expect object Platform {
  fun currentTimeMillis(): Long
  fun currentThreadId(): String
  fun ensureNeverFrozen(obj: Any)
  fun isFrozen(obj: Any): Boolean
}