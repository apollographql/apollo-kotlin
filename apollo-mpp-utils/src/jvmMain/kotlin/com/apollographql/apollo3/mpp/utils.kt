package com.apollographql.apollo3.mpp

actual fun currentTimeMillis(): Long {
  return System.currentTimeMillis()
}

actual fun currentThreadId(): String {
  return Thread.currentThread().id.toString()
}

actual fun assertMainThreadOnNative() {
}

actual fun platform() = Platform.Jvm
