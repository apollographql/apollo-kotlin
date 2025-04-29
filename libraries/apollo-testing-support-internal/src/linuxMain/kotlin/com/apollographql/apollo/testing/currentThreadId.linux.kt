package com.apollographql.apollo.testing

import platform.posix.pthread_self

actual fun currentThreadId(): String {
  return pthread_self().toString()
}