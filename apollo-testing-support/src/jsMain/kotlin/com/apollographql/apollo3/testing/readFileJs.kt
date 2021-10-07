package com.apollographql.apollo3.testing

import fs.readFileSync
import kotlin.test.assertEquals

actual fun readFile(path: String): String {
  // Workaround for https://youtrack.jetbrains.com/issue/KT-49125
  val pathPrefix = "../../../../../tests/integration-tests/"
  val options = object: fs.`T$44` {
    override var encoding: String? = "utf8"
  }

  return readFileSync("$pathPrefix$path", options) as String
}

actual fun checkFile(actualText: String, path: String) {
  assertEquals(actualText, readFile(path))
}
