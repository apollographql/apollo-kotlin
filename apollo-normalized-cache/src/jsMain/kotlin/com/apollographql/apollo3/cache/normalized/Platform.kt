package com.apollographql.apollo3.cache.normalized

import kotlin.js.Date

actual object Platform {
  actual fun currentTimeMillis(): Long {
    return Date().getTime().toLong()
  }

  actual fun currentThreadId(): String {
    return "js"
  }

  actual fun ensureNeverFrozen(obj: Any) {
  }

  actual fun isFrozen(obj: Any) = false
}