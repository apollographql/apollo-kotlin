package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
actual fun shouldUpdateTestFixtures(): Boolean {
  if (System.getenv("updateTestFixtures") != null) {
    return true
  }

  return when (System.getProperty("updateTestFixtures")?.trim()) {
    "on", "true", "1" -> true
    else -> false
  }
}

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
actual val testsPath: String = "../"