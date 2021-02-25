package com.apollographql.apollo3.interceptor.cache

import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.cache.normalized.internal.ReadableStore
import com.apollographql.apollo3.cache.normalized.internal.WriteableStore

class ApolloStore(private val normalizedCache: NormalizedCache): ReadableStore, WriteableStore {
  override fun merge(recordCollection: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    return normalizedCache.merge(recordCollection, cacheHeaders)
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return normalizedCache.merge(record, cacheHeaders)
  }

  override fun read(key: String, cacheHeaders: CacheHeaders): Record? {
    return normalizedCache.loadRecord(key, cacheHeaders)
  }

  override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    return keys.mapNotNull { normalizedCache.loadRecord(it, cacheHeaders) }
  }
}