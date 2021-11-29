package com.apollographql.apollo3.testing

import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File
import java.io.FileNotFoundException

actual val HostFileSystem = FileSystem.SYSTEM

actual fun shouldUpdateTestFixtures(): Boolean {
  return when (System.getProperty("updateTestFixtures")?.trim()) {
    "on", "true", "1" -> true
    else -> false
  }
}

actual val testsPath: String = "../"