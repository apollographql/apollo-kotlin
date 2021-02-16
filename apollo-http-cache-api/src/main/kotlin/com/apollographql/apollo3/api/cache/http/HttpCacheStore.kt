package com.apollographql.apollo3.api.cache.http

import java.io.IOException

/**
 * CacheStore is an abstraction for a cache store that is used to read, modify or delete http responses.
 */
interface HttpCacheStore {
  /**
   * Returns [HttpCacheRecord] for the entry named [cacheKey] or null if it doesn't exist or is not currently readable.
   */
  @Throws(IOException::class)
  fun cacheRecord(cacheKey: String): HttpCacheRecord?

  /**
   * Returns a [HttpCacheRecordEditor] for the entry named [cacheKey] or null if another edit is in progress.
   */
  @Throws(IOException::class)
  fun cacheRecordEditor(cacheKey: String): HttpCacheRecordEditor?

  /**
   * Drops the entry for [cacheKey] if it exists and can be removed. If the entry for key is currently being edited,
   * that edit will complete normally but its value will not be stored.
   */
  @Throws(IOException::class)
  fun remove(cacheKey: String)

  /**
   * Closes the cache and deletes all of its stored values. This will delete all files in the cache directory including
   * files that weren't created by the cache.
   */
  @Throws(IOException::class)
  fun delete()
}
