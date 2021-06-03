package com.apollographql.apollo3.testing

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import platform.posix.SEEK_END
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.readdir
import platform.posix.rewind
import kotlin.test.assertEquals

actual fun readFile(path: String): String {
  val file = fopen(path, "r")
  check (file != null) {
    "Cannot open $path"
  }

  fseek(file, 0, SEEK_END)
  val size = ftell(file)
  rewind(file)

  return memScoped {
    val tmp = allocArray<ByteVar>(size + 1)
    fread(tmp, sizeOf<ByteVar>().convert(), size.convert(), file)
    // terminate the string
    tmp.set(size, 0)
    tmp.toKString()
  }
}

actual fun checkFile(actualText: String, path: String) {
  // This does not update the test fixture automatically, this is left to the JVM implementation
  assertEquals(readFile(path), actualText)
}

