package com.apollographql.apollo3.mpp

import kotlin.js.Date

actual fun currentTimeMillis(): Long {
  return Date().getTime().toLong()
}

actual fun currentThreadId(): String {
  return "js"
}

actual fun assertMainThreadOnNative() {
}

actual fun platform() = Platform.Js
