package com.apollographql.apollo3.integration

import java.io.File

actual fun fixtureResponse(name: String): String {
  return File(System.getProperty("user.dir"), "../../apollo-integration/src/test/resources/$name").readText()
}