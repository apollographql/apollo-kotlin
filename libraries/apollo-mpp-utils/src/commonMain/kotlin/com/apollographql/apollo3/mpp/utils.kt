@file:JvmName("-utils")

package com.apollographql.apollo3.mpp

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_4_1
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

@Deprecated("With the new Memory Manager this method is no longer needed and is a no-op", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v3_4_1)
expect fun ensureNeverFrozen(@Suppress("UNUSED_PARAMETER") obj: Any)

@Deprecated("With the new Memory Manager this method is no longer needed and always return false", ReplaceWith("false"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v3_4_1)
expect fun isFrozen(@Suppress("UNUSED_PARAMETER") obj: Any): Boolean

@Deprecated("With the new Memory Manager this method is no longer needed and is a no-op", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v3_4_1)
expect fun freeze(@Suppress("UNUSED_PARAMETER") obj: Any)

@Deprecated("With the new Memory Manager this method is no longer needed and is a no-op", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v3_4_1)
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
//fun log(message: String) {
//  println("${currentTimeFormatted()} [${currentThreadName()}] $message")
//}
