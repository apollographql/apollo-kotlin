package com.apollographql.apollo.cache.normalized.lru;

import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class LruNormalizedCacheFactory implements NormalizedCacheFactory<LruNormalizedCache> {

  private final EvictionPolicy evictionPolicy;
  private final Optional<NormalizedCacheFactory> optionalSecondaryCache;

  public LruNormalizedCacheFactory(EvictionPolicy evictionPolicy) {
    this(evictionPolicy, null);
  }

  /**
   * @param evictionPolicy        The {@link EvictionPolicy} to manage the primary cache.
   * @param secondaryCacheFactory A {@link NormalizedCacheFactory} to create a secondary cache.
   */
  public LruNormalizedCacheFactory(EvictionPolicy evictionPolicy, NormalizedCacheFactory secondaryCacheFactory) {
    this.evictionPolicy = checkNotNull(evictionPolicy, "evictionPolicy == null");
    this.optionalSecondaryCache = Optional.fromNullable(secondaryCacheFactory);
  }

  @Override public LruNormalizedCache createNormalizedCache(RecordFieldJsonAdapter fieldAdapter) {
    return new LruNormalizedCache(fieldAdapter, evictionPolicy, optionalSecondaryCache);
  }
}
