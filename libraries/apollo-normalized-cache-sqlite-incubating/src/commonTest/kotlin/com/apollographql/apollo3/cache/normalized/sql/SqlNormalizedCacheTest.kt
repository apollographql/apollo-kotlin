package com.apollographql.apollo3.cache.normalized.sql

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.DefaultRecordMerger
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.sql.internal.JsonRecordDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.json.JsonQueries
import com.apollographql.apollo3.exception.apolloExceptionHandler
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlNormalizedCacheTest {

  private val cache: NormalizedCache = SqlNormalizedCacheFactory(null, false).create()

  @BeforeTest
  fun setUp() {
    cache.clearAll()
  }

  @Test
  fun testRecordCreation() {
    createRecord(STANDARD_KEY)
    assertNotNull(cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE))
  }

  @Test
  fun testRecordCreation_root() {
    createRecord(QUERY_ROOT_KEY)
    assertNotNull(cache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE))
  }

  @Test
  fun testRecordSelection() {
    createRecord(STANDARD_KEY)
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNotNull(record)
    assertEquals(expected = STANDARD_KEY, actual = record.key)
  }

  @Test
  fun testMultipleRecordSelection() {
    createRecord(STANDARD_KEY)
    createRecord(QUERY_ROOT_KEY)
    val selectionKeys = setOf(STANDARD_KEY, QUERY_ROOT_KEY)
    val records = cache.loadRecords(selectionKeys, CacheHeaders.NONE)
    val selectedKeys = records.map { it.key }.toSet()
    assertEquals(selectionKeys, selectedKeys)
  }

  @Test
  fun testRecordSelection_root() {
    createRecord(QUERY_ROOT_KEY)
    val record = requireNotNull(cache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE))
    assertNotNull(record)
    assertEquals(expected = QUERY_ROOT_KEY, actual = record.key)
  }

  @Test
  fun testRecordSelection_recordNotPresent() {
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testRecordMerge() {
    cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "fieldKey" to "valueUpdated",
                "newFieldKey" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
        recordMerger = DefaultRecordMerger,
    )
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNotNull(record)
    assertEquals(expected = "valueUpdated", actual = record.fields["fieldKey"])
    assertEquals(expected = true, actual = record.fields["newFieldKey"])
  }

  @Test
  fun testRecordDelete() {
    createRecord(STANDARD_KEY)
    cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "fieldKey" to "valueUpdated",
                "newFieldKey" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
        recordMerger = DefaultRecordMerger,
    )
    cache.remove(cacheKey = CacheKey(STANDARD_KEY), cascade = false)
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testClearAll() {
    createRecord(QUERY_ROOT_KEY)
    createRecord(STANDARD_KEY)
    cache.clearAll()
    assertNull(cache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE))
    assertNull(cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE))
  }

  // Tests for StandardCacheHeader compliance
  @Test
  fun testHeader_evictAfterRead() {
    createRecord(STANDARD_KEY)
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.builder()
        .addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
    assertNotNull(record)
    val nullRecord = cache.loadRecord(STANDARD_KEY, CacheHeaders.builder()
        .addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
    assertNull(nullRecord)
  }

  @Test
  fun testHeader_evictAfterBatchRead() {
    createRecord(STANDARD_KEY)
    createRecord(QUERY_ROOT_KEY)
    val selectionSet = setOf(STANDARD_KEY, QUERY_ROOT_KEY)
    val records = cache.loadRecords(selectionSet, CacheHeaders.builder()
        .addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
    assertEquals(records.size, 2)
    val emptyRecords = cache.loadRecords(selectionSet, CacheHeaders.builder()
        .addHeader(ApolloCacheHeaders.EVICT_AFTER_READ, "true").build())
    assertTrue(emptyRecords.isEmpty())
  }

  @Test
  fun testHeader_noCache() {
    cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = emptyMap(),
        ),
        cacheHeaders = CacheHeaders.builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build(),
        recordMerger = DefaultRecordMerger,
    )
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testRecordMerge_noOldRecord() {
    val changedKeys = cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "fieldKey" to "valueUpdated",
                "newFieldKey" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
        recordMerger = DefaultRecordMerger,
    )
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNotNull(record)
    assertEquals(expected = setOf("$STANDARD_KEY.fieldKey", "$STANDARD_KEY.newFieldKey"), actual = changedKeys)
    assertEquals(expected = "valueUpdated", actual = record.fields["fieldKey"])
    assertEquals(expected = true, actual = record.fields["newFieldKey"])
  }

  @Test
  fun testRecordMerge_withOldRecord() {
    createRecord(STANDARD_KEY)
    cache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "fieldKey" to "valueUpdated",
                "newFieldKey" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
        recordMerger = DefaultRecordMerger,
    )
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNotNull(record)
    assertEquals(expected = "valueUpdated", actual = record.fields["fieldKey"])
    assertEquals(expected = true, actual = record.fields["newFieldKey"])
  }

  @Test
  fun testPatternRemove() {
    createRecord("specialKey1")
    createRecord("specialKey2")
    createRecord("regularKey1")

    cache.remove("specialKey%")
    assertNull(cache.loadRecord("specialKey1", CacheHeaders.NONE))
    assertNull(cache.loadRecord("specialKey1", CacheHeaders.NONE))
    assertNotNull(cache.loadRecord("regularKey1", CacheHeaders.NONE))
  }

  @Test
  fun testPatternRemoveWithEscape() {
    createRecord("%1")

    cache.remove("\\%%")
    assertNull(cache.loadRecord("%1", CacheHeaders.NONE))
  }

  @Test
  fun exceptionCallsExceptionHandler() {
    val badCache = SqlNormalizedCache(JsonRecordDatabase(JsonQueries(BadDriver)))
    var throwable: Throwable? = null
    apolloExceptionHandler = {
      throwable = it
    }

    badCache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertEquals("Unable to read a record from the database", throwable!!.message)
    assertEquals("bad cache", throwable!!.cause!!.message)

    throwable = null
    badCache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "fieldKey" to "valueUpdated",
                "newFieldKey" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
        recordMerger = DefaultRecordMerger,
    )
    assertEquals("Unable to merge a record from the database", throwable!!.message)
    assertEquals("bad cache", throwable!!.cause!!.message)
  }

  private val BadDriver = object : SqlDriver {
    override fun close() {
      throw IllegalStateException("bad cache")
    }

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
      throw IllegalStateException("bad cache")
    }

    override fun currentTransaction(): Transacter.Transaction? {
      throw IllegalStateException("bad cache")
    }

    override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> {
      throw IllegalStateException("bad cache")
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      throw IllegalStateException("bad cache")
    }

    override fun newTransaction(): QueryResult<Transacter.Transaction> {
      throw IllegalStateException("bad cache")
    }

    override fun notifyListeners(vararg queryKeys: String) {
      throw IllegalStateException("bad cache")
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
      throw IllegalStateException("bad cache")
    }
  }

  private fun createRecord(key: String) {
    cache.merge(
        record = Record(
            key = key,
            fields = mapOf(
                "field1" to "value1",
                "field2" to "value2",
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
        recordMerger = DefaultRecordMerger,
    )
  }

  companion object {
    const val STANDARD_KEY = "key"
    const val QUERY_ROOT_KEY = "QUERY_ROOT"
  }
}
