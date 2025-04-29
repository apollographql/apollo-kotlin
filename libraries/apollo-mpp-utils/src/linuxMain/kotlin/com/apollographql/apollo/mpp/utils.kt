package com.apollographql.apollo.mpp

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.timespec

actual fun currentTimeMillis(): Long = memScoped {
  val timespec = alloc<timespec>()
  clock_gettime(CLOCK_REALTIME, timespec.ptr)
  return timespec.tv_sec * 1000 + (timespec.tv_nsec + 500_000) / 1_000_000
}
