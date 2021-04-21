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

actual fun fixtureResponse(name: String): String {
  val file = fopen("../integration-tests/testFixtures/$name", "r")
  println("cwd is ${cwd()}")
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
