package com.apollographql.apollo.network

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.appendBytes

fun ByteArray.toNSData(): NSData = NSMutableData().apply {
  if (isEmpty()) return@apply
  this@toNSData.usePinned {
    appendBytes(it.addressOf(0), size.convert())
  }
}
