package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.Logger;
import com.apollographql.apollo.NamedCountDownLatch;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;
import com.apollographql.apollo.internal.ApolloLogger;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ApolloStoreTest {

  @Test public void storeClearAllCallsNormalizedCacheClearAll() throws Exception {
    final NamedCountDownLatch latch = new NamedCountDownLatch("storeClearAllCallsNormalizedCacheClearAll", 1);
    final RealApolloStore realApolloStore = new RealApolloStore(new NormalizedCache() {
      @Nullable @Override public Record loadRecord(@Nonnull String key, @Nonnull CacheHeaders cacheHeaders) {
        return null;
      }

      @Nonnull @Override public Set<String> merge(@Nonnull Record record, @Nonnull CacheHeaders cacheHeaders) {
        return null;
      }

      @Override public void clearAll() {
        latch.countDown();
      }

      @Override public boolean remove(@Nonnull CacheKey cacheKey) {
        return false;
      }
    }, CacheKeyResolver.DEFAULT, new ScalarTypeAdapters(Collections.EMPTY_MAP), Executors.newSingleThreadExecutor(),
        new ApolloLogger(Optional.<Logger>absent()));
    realApolloStore.clearAll().execute();
    latch.awaitOrThrowWithTimeout(3, TimeUnit.SECONDS);
  }

}
