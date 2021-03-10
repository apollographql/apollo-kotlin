package com.apollographql.apollo3.integration

import java.io.File

actual fun fixtureResponse(name: String): String {
  return File("../integration-tests/src/test/resources/$name").readText()
}