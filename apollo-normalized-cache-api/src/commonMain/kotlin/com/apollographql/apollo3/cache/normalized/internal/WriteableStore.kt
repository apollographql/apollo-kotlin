package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.Record

interface WriteableStore : ReadableStore {
  fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String>
  fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String>
}
