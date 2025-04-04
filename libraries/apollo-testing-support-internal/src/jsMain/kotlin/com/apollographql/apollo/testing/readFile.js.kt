package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import okio.FileSystem
import okio.NodeJsFileSystem

actual val HostFileSystem: FileSystem = NodeJsFileSystem

actual fun shouldUpdateTestFixtures(): Boolean = false

// Workaround for https://youtrack.jetbrains.com/issue/KT-49125
actual val testsPath: String = "../../../../../tests/"