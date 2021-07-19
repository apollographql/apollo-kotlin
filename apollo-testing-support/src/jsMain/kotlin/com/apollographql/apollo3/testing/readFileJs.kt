package com.apollographql.apollo3.testing

import __dirname
import fs.readFileSync
import kotlin.test.assertEquals

actual fun readFile(path: String): String {
  println(__dirname)
  return readFileSync(path, null as String?) as String
}

actual fun checkFile(actualText: String, path: String) {
  assertEquals(actualText, readFile(path))
}
