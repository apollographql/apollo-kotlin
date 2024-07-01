package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloInternal
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.pthread_self

@ApolloInternal
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This function is not Apollo specific and will be removed in a future version. Copy/paste it in your codebase if you need it")
@OptIn(ExperimentalForeignApi::class)
actual fun currentThreadId(): String {
  return pthread_self()?.rawValue.toString()
}