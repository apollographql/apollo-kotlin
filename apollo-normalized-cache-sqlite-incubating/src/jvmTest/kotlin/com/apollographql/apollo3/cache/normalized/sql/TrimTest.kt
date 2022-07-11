package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.sql.TrimmableNormalizedCacheFactory
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrimTest {
  @Test
  fun trimTest() {
    val dbName = "build/test.db"
    val dbUrl = "jdbc:sqlite:$dbName"
    val dbFile = File(dbName)

    dbFile.delete()

    val cache = TrimmableNormalizedCacheFactory(dbUrl).create()

    val largeString = "".padStart(1024, '?')

    val oldRecord = Record(
        key = "old",
        fields = mapOf("key" to "value"),
        mutationId = null,
        date = mapOf("key" to 0L),
        metadata = emptyMap()
    )
    cache.merge(oldRecord, CacheHeaders.NONE)

    val newRecords = 0.until(2*1024).map {
      Record(
          key = "new$it",
          fields = mapOf("key" to largeString),
          mutationId = null,
          date = mapOf("key" to 1 + it.toLong()),
          metadata = emptyMap()
      )
    }
    cache.merge(newRecords, CacheHeaders.NONE)

    assertEquals(9596928, dbFile.length())

    // Trim the cache by 10%
    val trimmedCache = TrimmableNormalizedCacheFactory(dbUrl, 9596928, 0.1f).create()

    assertEquals(8548352, dbFile.length())
    // The oldest key must have been removed
    assertNull(trimmedCache.loadRecord("old", CacheHeaders.NONE))
  }
}