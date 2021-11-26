package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import okio.FileSystem

/**
 * reads a file in the testFixtures/ folder
 */
@ApolloExperimental
expect val HostFileSystem: FileSystem

@ApolloExperimental
expect fun shouldUpdateTestFixtures(): Boolean

