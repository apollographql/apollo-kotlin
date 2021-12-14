package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import okio.FileSystem

/**
 * The host filesystem
 */
@ApolloExperimental
expect val HostFileSystem: FileSystem

@ApolloExperimental
expect fun shouldUpdateTestFixtures(): Boolean

/**
 * The path to the "tests" directory. This assumes all tests are run from a predictable place relative to "tests"
 * We need this for JS tests where the CWD is not properly set at the beginning of tests
 */
@ApolloExperimental
expect val testsPath: String

