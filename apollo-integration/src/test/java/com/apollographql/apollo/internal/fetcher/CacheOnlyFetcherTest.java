package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_ONLY;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.google.common.truth.Truth.assertThat;

public class CacheOnlyFetcherTest extends BaseFetcherTest {

  @Test public void execute() throws IOException, ApolloException, InterruptedException {

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    Response<EpisodeHeroNameQuery.Data> responseData;

    // Is null when cache empty
    responseData = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(responseData.data()).isNull();
    assertThat(responseData.fromCache()).isTrue();
    assertThat(server.getRequestCount()).isEqualTo(0);

    // Populate cache
    server.enqueue(mockResponse("HeroNameResponse.json"));
    responseData = apolloClient.query(query).responseFetcher(NETWORK_ONLY).execute();
    assertThat(responseData.hasErrors()).isFalse();
    assertThat(responseData.data().hero().name()).isEqualTo("R2-D2");
    assertThat(server.getRequestCount()).isEqualTo(1);

    // Success after cache populated
    server.enqueue(mockResponse("HeroNameResponse.json"));
    responseData = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(responseData.hasErrors()).isFalse();
    assertThat(responseData.fromCache()).isTrue();
    assertThat(responseData.data().hero().name()).isEqualTo("R2-D2");
    assertThat(server.getRequestCount()).isEqualTo(1);

  }

  @Test public void enqueue() throws IOException, ApolloException, TimeoutException, InterruptedException {
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    TrackingCallback trackingCallback;

    // Is null when cache empty
    trackingCallback = new TrackingCallback();
    apolloClient.query(query).responseFetcher(CACHE_ONLY).enqueue(trackingCallback);
    trackingCallback.completedOrErrorLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(trackingCallback.exceptions.size()).isEqualTo(0);
    assertThat(trackingCallback.responseList.size()).isEqualTo(1);
    assertThat(trackingCallback.responseList.get(0).fromCache()).isTrue();
    assertThat(trackingCallback.responseList.get(0).data()).isNull();
    assertThat(server.getRequestCount()).isEqualTo(0);

    // Populate cache
    server.enqueue(mockResponse("HeroNameResponse.json"));
    final Response<EpisodeHeroNameQuery.Data> responseData
        = apolloClient.query(query).responseFetcher(NETWORK_ONLY).execute();
    assertThat(responseData.hasErrors()).isFalse();
    assertThat(responseData.data().hero().name()).isEqualTo("R2-D2");
    assertThat(server.getRequestCount()).isEqualTo(1);

    // Success after cache populated
    server.enqueue(mockResponse("HeroNameResponse.json"));
    trackingCallback = new TrackingCallback();
    apolloClient.query(query).responseFetcher(CACHE_ONLY).enqueue(trackingCallback);
    trackingCallback.completedOrErrorLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(trackingCallback.exceptions.size()).isEqualTo(0);
    assertThat(trackingCallback.responseList.size()).isEqualTo(1);
    assertThat(trackingCallback.responseList.get(0).fromCache()).isTrue();
    assertThat(trackingCallback.responseList.get(0).data().hero().name()).isEqualTo("R2-D2");
    assertThat(server.getRequestCount()).isEqualTo(1);
  }
}
