package com.apollographql.apollo.testing

actual fun shouldUpdateTestFixtures(): Boolean {
  if (System.getenv("updateTestFixtures") != null) {
    return true
  }

  return when (System.getProperty("updateTestFixtures")?.trim()) {
    "on", "true", "1" -> true
    else -> false
  }
}

actual val testsPath: String = "../"