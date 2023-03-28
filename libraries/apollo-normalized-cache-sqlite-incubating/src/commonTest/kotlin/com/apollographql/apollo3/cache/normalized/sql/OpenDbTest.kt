package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.testing.HostFileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class OpenDbTest {
  @Test
  fun openingDbWithDifferentParametersFails() {
    val baseDir = "build/testDbs"
    HostFileSystem.deleteRecursively(baseDir.toPath())
    HostFileSystem.createDirectories(baseDir.toPath())
    createCacheFactory(baseDir, false).create()
    try {
      createCacheFactory(baseDir, true).create()
      fail("an exception was expected")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("did you change the 'withDates' parameter?") ?: false)
    }
  }
}

expect fun createCacheFactory(baseDir: String, withDates: Boolean): SqlNormalizedCacheFactory