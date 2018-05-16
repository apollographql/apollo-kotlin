package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.IdFieldCacheKeyResolver;
import com.apollographql.apollo.Utils;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class BaseFetcherTest {

  ApolloClient apolloClient;
  MockWebServer server;

  @Before public void setUp() {
    server = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .writeTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .dispatcher(new Dispatcher(Utils.immediateExecutorService()))
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .dispatcher(Utils.immediateExecutor())
        .build();
  }

  @After public void shutdown() throws IOException {
    server.shutdown();
  }

  static class TrackingCallback extends ApolloCall.Callback<EpisodeHeroNameQuery.Data> {
    final List<Response<EpisodeHeroNameQuery.Data>> responseList = new ArrayList<>();
    final List<Exception> exceptions = new ArrayList<>();
    volatile boolean completed;

    @Override public void onResponse(@NotNull Response<EpisodeHeroNameQuery.Data> response) {
      if (completed) throw new IllegalStateException("onCompleted already called Do not reuse tracking callback.");
      responseList.add(response);
    }

    @Override public void onFailure(@NotNull ApolloException e) {
      if (completed) throw new IllegalStateException("onCompleted already called Do not reuse tracking callback.");
      exceptions.add(e);
    }

    @Override public void onStatusEvent(@NotNull ApolloCall.StatusEvent event) {
      if (event == ApolloCall.StatusEvent.COMPLETED) {
        if (completed) throw new IllegalStateException("onCompleted already called Do not reuse tracking callback.");
        completed = true;
      }
    }
  }

  MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }

}
