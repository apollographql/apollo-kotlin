package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import okio.FileSystem

/**
 * The host filesystem
 */
expect val HostFileSystem: FileSystem

expect fun shouldUpdateTestFixtures(): Boolean

/**
 * The path to the "tests" directory. This assumes all tests are run from a predictable place relative to "tests"
 * We need this for JS tests where the CWD is not properly set at the beginning of tests
 */
expect val testsPath: String

