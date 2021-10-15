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
 * The current platform. This is used from tests because Double.toString() doesn't behave the same on JS and other platforms.
 * Prefer more specific functions like `assertMainThreadOnNative` when possible instead of checking the platform.
 */
expect fun platform(): Platform

