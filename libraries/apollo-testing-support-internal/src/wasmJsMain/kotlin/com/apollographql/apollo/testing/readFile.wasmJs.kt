package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import okio.FileSystem

/**
 * The host filesystem
 */
actual val HostFileSystem: FileSystem
  get() = TODO("Not yet implemented")

actual fun shouldUpdateTestFixtures(): Boolean {
  TODO("Not yet implemented")
}

/**
 * The path to the "tests" directory. This assumes all tests are run from a predictable place relative to "tests"
 * We need this for JS tests where the CWD is not properly set at the beginning of tests
 */
actual val testsPath: String
  get() = TODO("Not yet implemented")