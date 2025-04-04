package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloInternal
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.pthread_self

actual fun currentThreadId(): String {
  return pthread_self()?.rawValue.toString()
}