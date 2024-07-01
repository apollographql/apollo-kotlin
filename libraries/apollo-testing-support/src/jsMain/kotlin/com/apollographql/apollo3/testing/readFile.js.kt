package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import okio.FileSystem
import okio.NodeJsFileSystem

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This function is not Apollo specific and will be removed in a future version. Copy/paste it in your codebase if you need it")
actual val HostFileSystem: FileSystem = NodeJsFileSystem

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
actual fun shouldUpdateTestFixtures(): Boolean = false

// Workaround for https://youtrack.jetbrains.com/issue/KT-49125
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
actual val testsPath: String = "../../../../../tests/"