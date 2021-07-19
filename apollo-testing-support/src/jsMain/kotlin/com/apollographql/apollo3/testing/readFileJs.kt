package com.apollographql.apollo3.testing

import fs.readFileSync
import kotlin.test.assertEquals

actual fun readFile(path: String): String {
  val pathPrefix = "../../../../../composite/tests/integration-tests/"
  return readFileSync("$pathPrefix$path", object: fs.`T$44` {
    override var encoding: String? = "utf8"
  }) as String
}

actual fun checkFile(actualText: String, path: String) {
  assertEquals(actualText, readFile(path))
}
