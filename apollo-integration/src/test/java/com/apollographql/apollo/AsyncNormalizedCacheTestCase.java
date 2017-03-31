package com.apollographql.apollo;

import com.google.common.base.Strings;

import android.support.annotation.NonNull;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

public class AsyncNormalizedCacheTestCase {
    private ApolloClient apolloClient;
  private MockWebServer server;
  private InMemoryCacheStore cacheStore;

  @Before public void setUp() {
    server = new MockWebServer();

    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    cacheStore = new InMemoryCacheStore();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(cacheStore, new CacheKeyResolver<Map<String, Object>>() {
          @Nonnull @Override public CacheKey resolve(@NonNull Map<String, Object> jsonObject) {
            String id = (String) jsonObject.get("id");
            if (Strings.isNullOrEmpty(id)) {
              return CacheKey.NO_KEY;
            }
            return CacheKey.from(id);
          }
        })
        .build();
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }

  @Test public void testAsync() throws IOException, InterruptedException {
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    server.enqueue(mockResponse("HeroNameResponse.json"));
    Response<EpisodeHeroName.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.isSuccessful()).isTrue();

    for (int i = 0; i < 500; i++) {
      server.enqueue(mockResponse("HeroNameResponse.json"));
    }

    final CountDownLatch latch = new CountDownLatch(1000);
    for (int i = 0; i < 1000; i++) {
      apolloClient.newCall(query).cacheControl(i % 2 == 0 ? CacheControl.NETWORK_FIRST : CacheControl.CACHE_ONLY)
          .enqueue(new ApolloCall.Callback<EpisodeHeroName.Data>() {
            @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
              assertThat(response.isSuccessful()).isTrue();
              latch.countDown();
            }

            @Override public void onFailure(@Nonnull Throwable e) {
              fail("unexpected error: " + e);
              latch.countDown();
            }
          });
    }

    latch.await(5, TimeUnit.SECONDS);
  }
}
