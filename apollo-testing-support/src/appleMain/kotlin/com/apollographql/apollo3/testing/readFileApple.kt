package com.apollographql.apollo3.testing

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.test.assertEquals

actual val HostFileSystem: FileSystem = FileSystem.SYSTEM

actual fun shouldUpdateTestFixtures(): Boolean = false

actual val testsPath: String = "../"
