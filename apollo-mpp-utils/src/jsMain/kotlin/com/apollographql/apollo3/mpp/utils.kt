package com.apollographql.apollo3.mpp

import kotlin.js.Date

actual fun currentTimeMillis(): Long {
  return Date().getTime().toLong()
}

actual fun currentTimeFormatted(): String {
  return Date().toISOString()
}

actual fun currentThreadId(): String {
  return "js"
}

actual fun currentThreadName(): String {
  return currentThreadId()
}

actual fun platform() = Platform.Js
