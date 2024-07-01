package com.apollographql.apollo.mpp

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long {
  return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
