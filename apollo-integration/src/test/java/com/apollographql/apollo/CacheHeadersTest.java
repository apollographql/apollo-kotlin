package com.apollographql.apollo;

import android.support.annotation.NonNull;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.cache.ApolloCacheHeaders;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.rx2.Rx2Apollo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptySet;

public class CacheHeadersTest {
  private static final int TIME_OUT_SECONDS = 3;

  private MockWebServer server;

  @Before
  public void setUp() {
    server = new MockWebServer();
  }

  @After
  public void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testHeadersReceived() throws ApolloException, IOException {
    final AtomicBoolean hasHeader = new AtomicBoolean();
    final NormalizedCache normalizedCache = new NormalizedCache() {
      @Nullable @Override public Record loadRecord(@NonNull String key, @NonNull CacheHeaders cacheHeaders) {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE));
        return null;
      }

      @Nonnull @Override public Set<String> merge(@NonNull Record record, @NonNull CacheHeaders cacheHeaders) {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE));
        return emptySet();
      }

      @Override public void clearAll() {
      }

      @Override public boolean remove(@Nonnull CacheKey cacheKey) {
        return false;
      }

      @Nonnull @Override
      protected Set<String> performMerge(@Nonnull Record apolloRecord, @Nonnull CacheHeaders cacheHeaders) {
        return emptySet();
      }
    };

    final NormalizedCacheFactory<NormalizedCache> cacheFactory = new NormalizedCacheFactory<NormalizedCache>() {
      @Override public NormalizedCache create(RecordFieldJsonAdapter recordFieldAdapter) {
        return normalizedCache;
      }
    };

    ApolloClient apolloClient = ApolloClient.builder()
        .normalizedCache(cacheFactory, new IdFieldCacheKeyResolver())
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient())
        .build();

    server.enqueue(mockResponse("HeroAndFriendsNameResponse.json"));
    CacheHeaders cacheHeaders = CacheHeaders.builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build();
    Rx2Apollo.from(apolloClient.query(new HeroAndFriendsNamesQuery(Input.fromNullable(Episode.NEWHOPE)))
        .cacheHeaders(cacheHeaders))
        .test().awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    assertThat(hasHeader.get()).isTrue();
  }

  @Test
  public void testDefaultHeadersReceived() throws IOException, ApolloException {
    final AtomicBoolean hasHeader = new AtomicBoolean();
    final NormalizedCache normalizedCache = new NormalizedCache() {
      @Nullable @Override public Record loadRecord(@NonNull String key, @NonNull CacheHeaders cacheHeaders) {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE));
        return null;
      }

      @Nonnull @Override public Set<String> merge(@NonNull Record record, @NonNull CacheHeaders cacheHeaders) {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE));
        return emptySet();
      }

      @Override public void clearAll() {
      }

      @Override public boolean remove(@Nonnull CacheKey cacheKey) {
        return false;
      }

      @Nonnull @Override
      protected Set<String> performMerge(@Nonnull Record apolloRecord, @Nonnull CacheHeaders cacheHeaders) {
        return emptySet();
      }
    };

    final NormalizedCacheFactory<NormalizedCache> cacheFactory = new NormalizedCacheFactory<NormalizedCache>() {
      @Override public NormalizedCache create(RecordFieldJsonAdapter recordFieldAdapter) {
        return normalizedCache;
      }
    };

    CacheHeaders cacheHeaders = CacheHeaders.builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build();

    ApolloClient apolloClient = ApolloClient.builder()
        .normalizedCache(cacheFactory, new IdFieldCacheKeyResolver())
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient())
        .defaultCacheHeaders(cacheHeaders)
        .build();

    server.enqueue(mockResponse("HeroAndFriendsNameResponse.json"));
    Rx2Apollo.from(apolloClient.query(new HeroAndFriendsNamesQuery(Input.fromNullable(Episode.NEWHOPE)))
        .cacheHeaders(cacheHeaders))
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    assertThat(hasHeader.get()).isTrue();
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }

}
