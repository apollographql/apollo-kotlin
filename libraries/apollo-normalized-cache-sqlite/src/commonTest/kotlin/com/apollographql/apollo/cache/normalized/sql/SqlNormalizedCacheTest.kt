package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.sql.internal.JsonRecordDatabase
import com.apollographql.apollo.cache.normalized.sql.internal.json.JsonQueries
import com.apollographql.apollo.cache.normalized.sql.internal.json.RecordForKey
import com.apollographql.apollo.cache.normalized.sql.internal.json.Records
import com.apollographql.apollo.cache.normalized.sql.internal.json.RecordsForKeys
import com.apollographql.apollo.exception.apolloExceptionHandler
import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransactionWithReturn
import app.cash.sqldelight.TransactionWithoutReturn
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlNormalizedCacheTest {

  private val cache: NormalizedCache = SqlNormalizedCacheFactory(null).create()

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
    )
    cache.remove(cacheKey = CacheKey(STANDARD_KEY), cascade = false)
    val record = cache.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testSqlThenMemoryMergeAndDeleteCascade() {
    val chainedCache = cache.chain(MemoryCacheFactory().create())
    testChainedMergeAndDeleteCascade(chainedCache)
  }

  @Test
  fun testMemoryThenSqlMergeAndDeleteCascade() {
    val chainedCache = MemoryCacheFactory().create().chain(cache)
    testChainedMergeAndDeleteCascade(chainedCache)
  }

  private fun testChainedMergeAndDeleteCascade(chainedCache: NormalizedCache) {
    chainedCache.merge(
        record = Record(
            key = "referencedKey",
            fields = mapOf(
                "field1" to true,
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
    )
    chainedCache.merge(
        record = Record(
            key = STANDARD_KEY,
            fields = mapOf(
                "field1" to "value1",
                "field2" to CacheKey("referencedKey"),
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
    )
    chainedCache.onEach {
      val record1 = it.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
      assertNotNull(record1)
      val record2 = it.loadRecord("referencedKey", CacheHeaders.NONE)
      assertNotNull(record2)
    }
    chainedCache.remove(cacheKey = CacheKey(STANDARD_KEY), cascade = true)
    chainedCache.onEach {
      val record1 = it.loadRecord(STANDARD_KEY, CacheHeaders.NONE)
      assertNull(record1)
      val record2 = it.loadRecord("referencedKey", CacheHeaders.NONE)
      assertNull(record2)
    }
  }

  private fun NormalizedCache.onEach(block: (NormalizedCache) -> Unit): NormalizedCache? {
    var c: NormalizedCache? = this
    while (c != null) {
      block(c)
      c = c.nextCache
    }
    return c
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
        cacheHeaders = CacheHeaders.NONE
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
  fun testManyRecords() {
    val records = 0.until(1001).map {
      Record(
          it.toString(),
          emptyMap()
      )
    }

    cache.merge(records, CacheHeaders.NONE)
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
    )
    assertEquals("Unable to merge a record from the database", throwable!!.message)
    assertEquals("bad cache", throwable!!.cause!!.message)
  }

  @Test
  fun testCascadeDeleteWithSelfReference() {
    // Creating a self-referencing record
    cache.merge(
        record = Record(
            key = "selfRefKey",
            fields = mapOf(
                "field1" to "value1",
                "selfRef" to CacheKey("selfRefKey"),
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
    )

    val result = cache.remove(cacheKey = CacheKey("selfRefKey"), cascade = true)

    assertTrue(result)
    val record = cache.loadRecord("selfRefKey", CacheHeaders.NONE)
    assertNull(record)
  }

  @Test
  fun testCascadeDeleteWithCyclicReferences() {
    // Creating two records that reference each other
    cache.merge(
        record = Record(
            key = "key1",
            fields = mapOf(
                "field1" to "value1",
                "refToKey2" to CacheKey("key2"),
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
    )

    cache.merge(
        record = Record(
            key = "key2",
            fields = mapOf(
                "field1" to "value2",
                "refToKey1" to CacheKey("key1"),
            ),
        ),
        cacheHeaders = CacheHeaders.NONE,
    )

    val result = cache.remove(cacheKey = CacheKey("key1"), cascade = true)

    assertTrue(result)
    assertNull(cache.loadRecord("key1", CacheHeaders.NONE))
    assertNull(cache.loadRecord("key2", CacheHeaders.NONE))
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
    )
  }

  companion object {
    const val STANDARD_KEY = "key"
    const val QUERY_ROOT_KEY = "QUERY_ROOT"
  }
}
