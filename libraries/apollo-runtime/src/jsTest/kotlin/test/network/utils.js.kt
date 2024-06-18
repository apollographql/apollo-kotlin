package test.network

import com.apollographql.apollo3.annotations.ApolloInternal

/**
 * The current platform. This is used from tests because Double.toString() doesn't behave the same on JS and other platforms.
 * Prefer more specific functions like `assertMainThreadOnNative` when possible instead of checking the platform.
 */
actual fun platform() = Platform.Js

