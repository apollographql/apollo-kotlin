package com.apollographql.apollo3.testing

import fs.readFileSync
import kotlin.test.assertEquals

actual fun readFile(path: String): String {
  val pathPrefix = "../../../../../composite/tests/integration-tests/"
  val pathPrefixKotlin = "../../../../../composite/tests/integration-tests-kotlin/"
  val options = object: fs.`T$44` {
    override var encoding: String? = "utf8"
  }

  return try {
    readFileSync("$pathPrefix$path", options)
  } catch (t: Throwable) {
    readFileSync("$pathPrefixKotlin$path", options)
  } as String
}

actual fun checkFile(actualText: String, path: String) {
  assertEquals(actualText, readFile(path))
}
