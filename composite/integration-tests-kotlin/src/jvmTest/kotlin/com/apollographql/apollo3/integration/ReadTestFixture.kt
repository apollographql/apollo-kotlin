package com.apollographql.apollo3.integration

import java.io.File

actual fun readTestFixture(name: String): String {
  return File("../integration-tests/testFixtures/$name").readText()
}