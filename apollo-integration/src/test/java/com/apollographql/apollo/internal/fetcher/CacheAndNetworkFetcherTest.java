package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.mockwebserver.MockResponse;

import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_AND_NETWORK;
import static com.google.common.truth.Truth.assertThat;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

public class CacheAndNetworkFetcherTest extends BaseFetcherTest {
  @Test public void executeThrowsException() throws IOException, ApolloException, InterruptedException {
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    // not supported
    boolean exception = false;
    try {
      apolloClient.query(query).responseFetcher(CACHE_AND_NETWORK).execute();
    } catch (IllegalStateException e) {
      exception = true;
    }
    assertThat(exception).isTrue();
  }

  @Test public void enqueue() throws IOException, ApolloException, TimeoutException, InterruptedException {
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    TrackingCallback trackingCallback;

    // Has error when cache empty, and network error
    server.enqueue(new MockResponse().setResponseCode(HTTP_INTERNAL_ERROR).setBody("Server Error"));
    trackingCallback = new TrackingCallback();
    apolloClient.query(query).responseFetcher(CACHE_AND_NETWORK).enqueue(trackingCallback);
    trackingCallback.completedOrErrorLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(trackingCallback.exceptions.size()).isEqualTo(1);

    // Goes to network when cache empty, one response
    server.enqueue(mockResponse("HeroNameResponse.json"));
    trackingCallback = new TrackingCallback();
    apolloClient.query(query).responseFetcher(CACHE_AND_NETWORK).enqueue(trackingCallback);
    trackingCallback.completedOrErrorLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(trackingCallback.exceptions).isEmpty();
    assertThat(trackingCallback.responseList.size()).isEqualTo(1);
    assertThat(trackingCallback.responseList.get(0).fromCache()).isFalse();
    assertThat(trackingCallback.responseList.get(0).data().hero().name()).isEqualTo("R2-D2");

    // Goes to network and cache after cache populated
    server.enqueue(mockResponse("HeroNameResponse.json"));
    trackingCallback = new TrackingCallback();
    apolloClient.query(query).responseFetcher(CACHE_AND_NETWORK).enqueue(trackingCallback);
    trackingCallback.completedOrErrorLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(trackingCallback.exceptions).isEmpty();
    assertThat(trackingCallback.responseList.size()).isEqualTo(2);

    // Cache is always first
    assertThat(trackingCallback.responseList.get(0).fromCache()).isTrue();
    assertThat(trackingCallback.responseList.get(0).data().hero().name()).isEqualTo("R2-D2");

    assertThat(trackingCallback.responseList.get(1).fromCache()).isFalse();
    assertThat(trackingCallback.responseList.get(1).data().hero().name()).isEqualTo("R2-D2");

    // Falls back to cache if network error
    server.enqueue(new MockResponse().setResponseCode(HTTP_INTERNAL_ERROR).setBody("Server Error"));
    trackingCallback = new TrackingCallback();
    apolloClient.query(query).responseFetcher(CACHE_AND_NETWORK).enqueue(trackingCallback);
    trackingCallback.completedOrErrorLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(trackingCallback.exceptions).isEmpty();
    assertThat(trackingCallback.responseList.size()).isEqualTo(1);
    assertThat(trackingCallback.responseList.get(0).fromCache()).isTrue();
    assertThat(trackingCallback.responseList.get(0).data().hero().name()).isEqualTo("R2-D2");
  }
}
