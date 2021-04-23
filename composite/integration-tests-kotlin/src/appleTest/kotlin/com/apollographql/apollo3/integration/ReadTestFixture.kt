package com.apollographql.apollo3.integration

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import platform.posix.SEEK_END
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.rewind
import kotlin.test.assertEquals

actual fun readTestFixture(name: String): String {
  val file = fopen("../integration-tests/testFixtures/$name", "r")
  check (file != null) {
    "Cannot open fixture $name"
  }

  fseek(file, 0, SEEK_END)
  val size = ftell(file)
  rewind(file)

  return memScoped {
    val tmp = allocArray<ByteVar>(size)
    fread(tmp, sizeOf<ByteVar>().convert(), size.convert(), file)
    tmp.toKString()
  }
}

actual fun checkTestFixture(actualText: String, name: String) {
  // This does not update the test fixture automatically, this is left to the JVM implementation
  assertEquals(actualText, readTestFixture(name))
}