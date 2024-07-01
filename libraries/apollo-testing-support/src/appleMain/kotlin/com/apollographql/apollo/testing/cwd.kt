package com.apollographql.apollo.testing

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.getcwd

fun cwd(): String {
  return memScoped {
    val maxSize = 1024
    val buf = allocArray<ByteVar>(maxSize)
    getcwd(buf, maxSize.convert())?.toKString() ?: "?"
  }
}