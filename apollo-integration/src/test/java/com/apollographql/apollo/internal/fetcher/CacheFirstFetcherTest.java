package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.mockwebserver.MockResponse;

import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_FIRST;
import static com.google.common.truth.Truth.assertThat;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

public class CacheFirstFetcherTest extends BaseFetcherTest {

  @Test public void execute() throws IOException, ApolloException {
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();

    // Has exception when cache empty and network error
    server.enqueue(new MockResponse().setResponseCode(HTTP_INTERNAL_ERROR).setBody("Server Error"));
    boolean exception = false;
    try {
      apolloClient.query(query).responseFetcher(CACHE_FIRST).execute();
    } catch (ApolloException e) {
      exception = true;
    }
    assertThat(exception).isTrue();
    assertThat(server.getRequestCount()).isEqualTo(1);

    // Goes to network first
    server.enqueue(mockResponse("HeroNameResponse.json"));
    Response<EpisodeHeroNameQuery.Data> responseData = apolloClient.query(query).execute();
    assertThat(responseData.hasErrors()).isFalse();
    assertThat(responseData.fromCache()).isFalse();
    assertThat(responseData.data().hero().name()).isEqualTo("R2-D2");
    assertThat(server.getRequestCount()).isEqualTo(2);

    // Hits only cache after populated
    responseData = apolloClient.query(query).responseFetcher(CACHE_FIRST).execute();
    assertThat(responseData.hasErrors()).isFalse();
    assertThat(responseData.fromCache()).isTrue();
    assertThat(responseData.data().hero().name()).isEqualTo("R2-D2");
    assertThat(server.getRequestCount()).isEqualTo(2);
  }

  @Test public void enqueue() throws IOException, ApolloException, TimeoutException, InterruptedException {

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    TrackingCallback trackingCallback;

    // Has error when cache empty, and network error
    server.enqueue(new MockResponse().setResponseCode(HTTP_INTERNAL_ERROR).setBody("Server Error"));
    trackingCallback = new TrackingCallback();
    apolloClient.query(query).responseFetcher(CACHE_FIRST).enqueue(trackingCallback);
    trackingCallback.completedOrErrorLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(trackingCallback.exceptions.size()).isEqualTo(1);

    // Goes to network when empty
    trackingCallback = new TrackingCallback();
    server.enqueue(mockResponse("HeroNameResponse.json"));
    apolloClient.query(query).responseFetcher(CACHE_FIRST).enqueue(trackingCallback);
    trackingCallback.completedOrErrorLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(trackingCallback.exceptions).isEmpty();
    assertThat(trackingCallback.responseList.size()).isEqualTo(1);
    assertThat(trackingCallback.responseList.get(0).fromCache()).isFalse();
    assertThat(trackingCallback.responseList.get(0).data().hero().name()).isEqualTo("R2-D2");
    assertThat(server.getRequestCount()).isEqualTo(2);

    // Hits only cache after populated
    trackingCallback = new TrackingCallback();
    apolloClient.query(query).responseFetcher(CACHE_FIRST).enqueue(trackingCallback);
    trackingCallback.completedOrErrorLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(trackingCallback.exceptions).isEmpty();
    assertThat(trackingCallback.responseList.size()).isEqualTo(1);
    assertThat(trackingCallback.responseList.get(0).fromCache()).isTrue();
    assertThat(trackingCallback.responseList.get(0).data().hero().name()).isEqualTo("R2-D2");
    assertThat(server.getRequestCount()).isEqualTo(2);
  }
}
