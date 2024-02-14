package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.benasher44.uuid.Uuid

interface OptimisticNormalizedCache : NormalizedCache {
  fun addOptimisticUpdates(recordSet: Collection<Record>): Set<String>

  fun addOptimisticUpdate(record: Record): Set<String>

  fun removeOptimisticUpdates(mutationId: Uuid): Set<String>
}
