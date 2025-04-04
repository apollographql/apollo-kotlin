package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

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