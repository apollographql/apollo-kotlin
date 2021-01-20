package com.apollographql.apollo.cache.normalized.lru

import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter

@Deprecated("Will be removed soon")
class LruNormalizedCacheFactory(
    /**
     * [EvictionPolicy] to manage the primary cache.
     */
    private val evictionPolicy: EvictionPolicy
) : NormalizedCacheFactory<LruNormalizedCache>() {

  override fun create(recordFieldAdapter: RecordFieldJsonAdapter): LruNormalizedCache =
      LruNormalizedCache(evictionPolicy)

}
