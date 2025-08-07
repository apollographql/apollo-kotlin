package com.apollographql.apollo.mpp

@Suppress("OPT_IN_USAGE")
private fun currentTimeMillis2(): Double = js("(new Date()).getTime()")
actual fun currentTimeMillis(): Long {
  return currentTimeMillis2().toLong()
}

