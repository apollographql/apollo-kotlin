package com.apollographql.apollo.internal;


import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.IdFieldCacheKeyResolver;
import com.apollographql.apollo.NamedCountDownLatch;
import com.apollographql.apollo.Utils;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.CreateReview;
import com.apollographql.apollo.integration.normalizer.ReviewsByEpisode;
import com.apollographql.apollo.integration.normalizer.type.ColorInput;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.integration.normalizer.type.ReviewInput;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

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

    final NamedCountDownLatch completionCountDownLatch = new NamedCountDownLatch("refetch_query_no_pre_cached", 1);
    RealApolloCall call = (RealApolloCall) apolloClient.mutate(mutation).refetchQueries(empireReviewsQuery);
    ((QueryReFetcher) call.queryReFetcher.get()).onCompleteCallback = new QueryReFetcher.OnCompleteCallback() {
      @Override public void onFetchComplete() {
        completionCountDownLatch.countDown();
      }
    };
    call.execute();
    completionCountDownLatch.awaitOrThrowWithTimeout(30000, TimeUnit.SECONDS);

    assertThat(server.getRequestCount()).isEqualTo(2);

    Response<ReviewsByEpisode.Data> empireReviewsQueryResponse = apolloClient.query(empireReviewsQuery).cacheControl
        (CacheControl.CACHE_ONLY).execute();
    assertThat(empireReviewsQueryResponse.data().reviews()).hasSize(3);
    assertThat(empireReviewsQueryResponse.data().reviews().get(2).stars()).isEqualTo(5);
    assertThat(empireReviewsQueryResponse.data().reviews().get(2).commentary()).isEqualTo("Amazing");
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

    final NamedCountDownLatch completionCountDownLatch = new NamedCountDownLatch("refetch_query_no_pre_cached", 1);
    RealApolloCall call = (RealApolloCall) apolloClient.mutate(mutation).refetchQueries(empireReviewsQuery);
    ((QueryReFetcher) call.queryReFetcher.get()).onCompleteCallback = new QueryReFetcher.OnCompleteCallback() {
      @Override public void onFetchComplete() {
        completionCountDownLatch.countDown();
      }
    };
    call.execute();
    completionCountDownLatch.awaitOrThrowWithTimeout(3, TimeUnit.SECONDS);

    assertThat(server.getRequestCount()).isEqualTo(3);

    empireReviewsQueryResponse = apolloClient.query(empireReviewsQuery).cacheControl
        (CacheControl.CACHE_ONLY).execute();
    assertThat(empireReviewsQueryResponse.data().reviews()).hasSize(4);
    assertThat(empireReviewsQueryResponse.data().reviews().get(3).stars()).isEqualTo(5);
    assertThat(empireReviewsQueryResponse.data().reviews().get(3).commentary()).isEqualTo("Awesome");
  }

  @Test public void refetch_watchers() throws Exception {
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponse.json"));
    server.enqueue(mockResponse("CreateReviewResponse.json"));
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponseUpdated.json"));

    final NamedCountDownLatch countDownBeforeMutationLatch = new NamedCountDownLatch("before_mutation", 1);
    final NamedCountDownLatch countDownRefetchLatch = new NamedCountDownLatch("refetch", 2);
    final AtomicReference<Response<ReviewsByEpisode.Data>> empireReviewsWatchResponse = new AtomicReference<>();
    ApolloQueryWatcher<ReviewsByEpisode.Data> queryWatcher = apolloClient.query(new ReviewsByEpisode(Episode.EMPIRE))
        .watcher()
        .refetchCacheControl(CacheControl.NETWORK_FIRST)
        .enqueueAndWatch(new ApolloCall.Callback<ReviewsByEpisode.Data>() {
          @Override public void onResponse(@Nonnull Response<ReviewsByEpisode.Data> response) {
            empireReviewsWatchResponse.set(response);
            countDownBeforeMutationLatch.countDown();
            countDownRefetchLatch.countDown();
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
          }
        });

    countDownBeforeMutationLatch.awaitOrThrowWithTimeout(3, TimeUnit.SECONDS);
    CreateReview mutation = new CreateReview(
        Episode.EMPIRE,
        ReviewInput.builder().stars(5).commentary("Awesome").favoriteColor(ColorInput.builder().build()).build()
    );
    apolloClient.mutate(mutation).refetchQueries(queryWatcher.operation().name()).execute();

    countDownRefetchLatch.awaitOrThrowWithTimeout(3, TimeUnit.SECONDS);
    assertThat(server.getRequestCount()).isEqualTo(3);
    Response<ReviewsByEpisode.Data> empireReviewsQueryResponse = empireReviewsWatchResponse.get();
    assertThat(empireReviewsQueryResponse.data().reviews()).hasSize(4);
    assertThat(empireReviewsQueryResponse.data().reviews().get(3).stars()).isEqualTo(5);
    assertThat(empireReviewsQueryResponse.data().reviews().get(3).commentary()).isEqualTo("Awesome");

    queryWatcher.cancel();
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
