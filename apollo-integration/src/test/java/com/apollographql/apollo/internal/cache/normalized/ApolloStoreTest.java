package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.Logger;
import com.apollographql.apollo.NamedCountDownLatch;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Collections.emptySet;

public class ApolloStoreTest {

  @Test public void storeClearAllCallsNormalizedCacheClearAll() throws Exception {
    final NamedCountDownLatch latch = new NamedCountDownLatch("storeClearAllCallsNormalizedCacheClearAll", 1);
    final RealApolloStore realApolloStore = new RealApolloStore(new NormalizedCache() {
      @Nullable @Override public Record loadRecord(@NotNull String key, @NotNull CacheHeaders cacheHeaders) {
        return null;
      }

      @NotNull @Override public Set<String> merge(@NotNull Record record, @NotNull CacheHeaders cacheHeaders) {
        return emptySet();
      }

      @Override public void clearAll() {
        latch.countDown();
      }

      @Override public boolean remove(@NotNull CacheKey cacheKey) {
        return false;
      }

      @NotNull @Override
      protected Set<String> performMerge(@NotNull Record apolloRecord, @NotNull CacheHeaders cacheHeaders) {
        return emptySet();
      }
    }, CacheKeyResolver.DEFAULT, new ScalarTypeAdapters(Collections.EMPTY_MAP), Executors.newSingleThreadExecutor(),
        new ApolloLogger(Optional.<Logger>absent()));
    realApolloStore.clearAll().execute();
    latch.awaitOrThrowWithTimeout(3, TimeUnit.SECONDS);
  }

}
