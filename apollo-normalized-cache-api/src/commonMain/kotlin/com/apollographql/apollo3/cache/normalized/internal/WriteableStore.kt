package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.Record

interface WriteableStore : ReadableStore {
  fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String>
  fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String>
}
