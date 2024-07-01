package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import okio.FileSystem

/**
 * The host filesystem
 */
@ApolloExperimental
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This function is not Apollo specific and will be removed in a future version. Copy/paste it in your codebase if you need it")
expect val HostFileSystem: FileSystem

@ApolloExperimental
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
expect fun shouldUpdateTestFixtures(): Boolean

/**
 * The path to the "tests" directory. This assumes all tests are run from a predictable place relative to "tests"
 * We need this for JS tests where the CWD is not properly set at the beginning of tests
 */
@ApolloExperimental
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
expect val testsPath: String

