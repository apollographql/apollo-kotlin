package com.apollographql.apollo;


import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.integration.normalizer.CreateReview;
import com.apollographql.apollo.integration.normalizer.ReviewsByEpisode;
import com.apollographql.apollo.integration.normalizer.type.ColorInput;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.integration.normalizer.type.ReviewInput;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class QueryRefetchTest {
  private ApolloClient apolloClient;
  private MockWebServer server;

  @Before public void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .build();
  }

  @After public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignored) {
    }
  }

  @Test public void refetch_query_no_pre_cached() throws Exception {
    server.enqueue(mockResponse("CreateReviewResponse.json"));
    CreateReview mutation = new CreateReview(
        Episode.EMPIRE,
        ReviewInput.builder().stars(5).commentary("Awesome").favoriteColor(ColorInput.builder().build()).build()
    );

    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponse.json"));
    ReviewsByEpisode empireReviewsQuery = new ReviewsByEpisode(Episode.EMPIRE);

    server.enqueue(mockResponse("ReviewsJediEpisodeResponse.json"));
    ReviewsByEpisode jediReviewsQuery = new ReviewsByEpisode(Episode.JEDI);

    apolloClient.mutate(mutation).refetchQueries(empireReviewsQuery, jediReviewsQuery).execute();
    assertThat(server.getRequestCount()).isEqualTo(3);

    Response<ReviewsByEpisode.Data> empireReviewsQueryResponse = apolloClient.query(empireReviewsQuery).cacheControl
        (CacheControl.CACHE_ONLY).execute();
    assertThat(empireReviewsQueryResponse.data().reviews()).hasSize(3);
    assertThat(empireReviewsQueryResponse.data().reviews().get(2).stars()).isEqualTo(5);
    assertThat(empireReviewsQueryResponse.data().reviews().get(2).commentary()).isEqualTo("Amazing");

    Response<ReviewsByEpisode.Data> jediReviewsQueryResponse = apolloClient.query(jediReviewsQuery).cacheControl
        (CacheControl.CACHE_ONLY).execute();
    assertThat(jediReviewsQueryResponse.data().reviews()).hasSize(1);
    assertThat(jediReviewsQueryResponse.data().reviews().get(0).stars()).isEqualTo(5);
    assertThat(jediReviewsQueryResponse.data().reviews().get(0).commentary()).isEqualTo("Fascinating");
  }

  @Test public void refetch_query_pre_cached() throws Exception {
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponse.json"));
    ReviewsByEpisode empireReviewsQuery = new ReviewsByEpisode(Episode.EMPIRE);
    apolloClient.query(empireReviewsQuery).cacheControl(CacheControl.NETWORK_FIRST).execute();

    Response<ReviewsByEpisode.Data> empireReviewsQueryResponse = apolloClient.query(empireReviewsQuery)
        .cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(empireReviewsQueryResponse.data().reviews()).hasSize(3);
    assertThat(empireReviewsQueryResponse.data().reviews().get(2).stars()).isEqualTo(5);
    assertThat(empireReviewsQueryResponse.data().reviews().get(2).commentary()).isEqualTo("Amazing");

    server.enqueue(mockResponse("CreateReviewResponse.json"));
    CreateReview mutation = new CreateReview(
        Episode.EMPIRE,
        ReviewInput.builder().stars(5).commentary("Awesome").favoriteColor(ColorInput.builder().build()).build()
    );

    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponseUpdated.json"));
    apolloClient.mutate(mutation).refetchQueries(empireReviewsQuery).execute();
    assertThat(server.getRequestCount()).isEqualTo(3);

    empireReviewsQueryResponse = apolloClient.query(empireReviewsQuery).cacheControl
        (CacheControl.CACHE_ONLY).execute();
    assertThat(empireReviewsQueryResponse.data().reviews()).hasSize(4);
    assertThat(empireReviewsQueryResponse.data().reviews().get(3).stars()).isEqualTo(5);
    assertThat(empireReviewsQueryResponse.data().reviews().get(3).commentary()).isEqualTo("Awesome");
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
