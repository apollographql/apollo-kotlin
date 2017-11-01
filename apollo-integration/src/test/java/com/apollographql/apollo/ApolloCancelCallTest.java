package com.apollographql.apollo;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.cache.http.ApolloHttpCache;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.rx2.Rx2Apollo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class ApolloCancelCallTest {
  private static final long TIME_OUT_SECONDS = 3;
  private ApolloClient apolloClient;
  private MockWebServer server;
  private MockHttpCacheStore cacheStore;

  @Before
  public void setup() {
    server = new MockWebServer();
    cacheStore = new MockHttpCacheStore();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .httpCache(new ApolloHttpCache(cacheStore, null))
        .build();
  }

  @After public void tearDown() {
    try {
      server.shutdown();
    } catch (Exception ignore) {
    }
  }

  @Test
  public void cancelCallBeforeEnqueueCanceledException() throws Exception {
    server.enqueue(mockResponse("EpisodeHeroNameResponse.json"));

    ApolloCall<EpisodeHeroNameQuery.Data> call = apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)));
    call.cancel();

    Rx2Apollo.from(call)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertError(ApolloCanceledException.class);
  }

  @Test
  public void cancelCallAfterEnqueueNoCallback() throws Exception {
    server.enqueue(mockResponse("EpisodeHeroNameResponse.json")
        .setBodyDelay(TIME_OUT_SECONDS / 2, TimeUnit.SECONDS));

    final ApolloCall<EpisodeHeroNameQuery.Data> call = apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)));

    new Thread(new Runnable() {
      @Override public void run() {
        try {
          Thread.sleep(500);
        } catch (Exception e) {
        }
        call.cancel();
      }
    }).start();

    Rx2Apollo.from(call)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertNoValues()
        .assertNotComplete();
  }

  @Test
  public void cancelPrefetchBeforeEnqueueCanceledException() throws Exception {
    server.enqueue(mockResponse("EpisodeHeroNameResponse.json"));

    final AtomicReference<ApolloException> errorRef = new AtomicReference<>();
    ApolloCall<EpisodeHeroNameQuery.Data> call = apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)));

    call.cancel();

    Rx2Apollo.from(call)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertError(ApolloCanceledException.class);
  }

  @Test
  public void cancelPrefetchAfterEnqueueNoCallback() throws Exception {
    server.enqueue(mockResponse("EpisodeHeroNameResponse.json")
        .setBodyDelay(TIME_OUT_SECONDS / 2, TimeUnit.SECONDS));

    final ApolloPrefetch call = apolloClient.prefetch(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)));

    new Thread(new Runnable() {
      @Override public void run() {
        try {
          Thread.sleep(200);
        } catch (Exception e) {
        }
        call.cancel();
      }
    }).start();

    Rx2Apollo.from(call)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertNotComplete();
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
