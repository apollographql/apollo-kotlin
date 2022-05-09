@file:JvmName("-utils")

package com.apollographql.apollo3.mpp

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlin.jvm.JvmName

expect fun currentTimeMillis(): Long

/**
 * The current time as a human-readable String. Used for debugging.
 */
@ApolloInternal
expect fun currentTimeFormatted(): String

expect fun currentThreadId(): String

/**
 * The current thread name ("main" for the main thread). Used for debugging.
 */
@ApolloInternal
expect fun currentThreadName(): String

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

// Helpful for debugging, but not wanted in the final library - uncomment as needed
//@OptIn(ApolloInternal::class)
//fun log(message: String) {
//  println("${currentTimeFormatted()} [${currentThreadName()}] $message")
//}
