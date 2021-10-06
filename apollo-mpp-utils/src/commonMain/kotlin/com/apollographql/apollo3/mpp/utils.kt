package com.apollographql.apollo3.mpp

expect fun currentTimeMillis(): Long
expect fun currentThreadId(): String
expect fun ensureNeverFrozen(obj: Any)
expect fun isFrozen(obj: Any): Boolean
expect fun freeze(obj: Any)
expect fun assertMainThreadOnNative()

enum class Platform {
  Jvm,
  Native,
  Js
}

/**
 * The current platform. Use as a last resort
 */
expect fun platform(): Platform

