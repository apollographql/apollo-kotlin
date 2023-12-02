package com.apollographql.apollo3.mpp

private fun currentTimeMillis2(): Double  = js("(new Date()).getTime()")
actual fun currentTimeMillis(): Long {
  return currentTimeMillis2().toLong()
}

actual fun currentTimeFormatted(): String = js("(new Date()).toISOString()")

actual fun currentThreadId(): String {
  return "wasm-js"
}

actual fun currentThreadName(): String {
  return currentThreadId()
}

actual fun platform() = Platform.WasmJs
