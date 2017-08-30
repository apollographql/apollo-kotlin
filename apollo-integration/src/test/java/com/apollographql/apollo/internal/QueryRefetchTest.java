package com.apollographql.apollo.internal;


import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.IdFieldCacheKeyResolver;
import com.apollographql.apollo.NamedCountDownLatch;
import com.apollographql.apollo.Utils;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.CreateReviewMutation;
import com.apollographql.apollo.integration.normalizer.ReviewsByEpisodeQuery;
import com.apollographql.apollo.integration.normalizer.type.ColorInput;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.integration.normalizer.type.ReviewInput;
import com.apollographql.apollo.rx2.Rx2Apollo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import io.reactivex.functions.Predicate;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.Utils.TIME_OUT_SECONDS;
import static com.apollographql.apollo.Utils.assertResponse;
import static com.apollographql.apollo.Utils.enqueueAndAssertResponse;
import static com.apollographql.apollo.Utils.mockResponse;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_ONLY;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_FIRST;
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

  @Test public void refetchNoPreCachedQuery() throws Exception {
    CreateReviewMutation mutation = new CreateReviewMutation(
        Episode.EMPIRE,
        ReviewInput.builder().stars(5).commentary("Awesome").favoriteColor(ColorInput.builder().build()).build()
    );

    server.enqueue(mockResponse("CreateReviewResponse.json"));
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponse.json"));

    final NamedCountDownLatch completionCountDownLatch = new NamedCountDownLatch("refetchNoPreCachedQuery", 1);
    RealApolloCall call = (RealApolloCall) apolloClient.mutate(mutation).refetchQueries(new ReviewsByEpisodeQuery(Episode.EMPIRE));
    ((QueryReFetcher) call.queryReFetcher.get()).onCompleteCallback = new QueryReFetcher.OnCompleteCallback() {
      @Override public void onFetchComplete() {
        completionCountDownLatch.countDown();
      }
    };

    Rx2Apollo
        .from(call)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    completionCountDownLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertResponse(
        apolloClient.query(new ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(CACHE_ONLY),
        new Predicate<Response<ReviewsByEpisodeQuery.Data>>() {
          @Override public boolean test(Response<ReviewsByEpisodeQuery.Data> response) throws Exception {
            assertThat(response.data().reviews()).hasSize(3);
            assertThat(response.data().reviews().get(2).stars()).isEqualTo(5);
            assertThat(response.data().reviews().get(2).commentary()).isEqualTo("Amazing");
            return true;
          }
        }
    );
  }

  @Test public void refetchPreCachedQuery() throws Exception {
    enqueueAndAssertResponse(
        server,
        "ReviewsEmpireEpisodeResponse.json",
        apolloClient.query(new ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(NETWORK_FIRST),
        new Predicate<Response<ReviewsByEpisodeQuery.Data>>() {
          @Override public boolean test(Response<ReviewsByEpisodeQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    assertResponse(
        apolloClient.query(new ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(CACHE_ONLY),
        new Predicate<Response<ReviewsByEpisodeQuery.Data>>() {
          @Override public boolean test(Response<ReviewsByEpisodeQuery.Data> response) throws Exception {
            assertThat(response.data().reviews()).hasSize(3);
            assertThat(response.data().reviews().get(2).stars()).isEqualTo(5);
            assertThat(response.data().reviews().get(2).commentary()).isEqualTo("Amazing");
            return true;
          }
        }
    );

    CreateReviewMutation mutation = new CreateReviewMutation(
        Episode.EMPIRE,
        ReviewInput.builder().stars(5).commentary("Awesome").favoriteColor(ColorInput.builder().build()).build()
    );

    server.enqueue(mockResponse("CreateReviewResponse.json"));
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponseUpdated.json"));

    final NamedCountDownLatch completionCountDownLatch = new NamedCountDownLatch("refetch_query_no_pre_cached", 1);
    RealApolloCall call = (RealApolloCall) apolloClient.mutate(mutation).refetchQueries(new ReviewsByEpisodeQuery(Episode.EMPIRE));
    ((QueryReFetcher) call.queryReFetcher.get()).onCompleteCallback = new QueryReFetcher.OnCompleteCallback() {
      @Override public void onFetchComplete() {
        completionCountDownLatch.countDown();
      }
    };

    Rx2Apollo
        .from(call)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    completionCountDownLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    assertThat(server.getRequestCount()).isEqualTo(3);

    assertResponse(
        apolloClient.query(new ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(CACHE_ONLY),
        new Predicate<Response<ReviewsByEpisodeQuery.Data>>() {
          @Override public boolean test(Response<ReviewsByEpisodeQuery.Data> response) throws Exception {
            assertThat(response.data().reviews()).hasSize(4);
            assertThat(response.data().reviews().get(3).stars()).isEqualTo(5);
            assertThat(response.data().reviews().get(3).commentary()).isEqualTo("Awesome");
            return true;
          }
        }
    );
  }

  @Test public void refetchWatchers() throws Exception {
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponse.json"));
    server.enqueue(mockResponse("CreateReviewResponse.json"));
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponseUpdated.json"));

    final NamedCountDownLatch countDownBeforeMutationLatch = new NamedCountDownLatch("before_mutation", 1);
    final NamedCountDownLatch countDownRefetchLatch = new NamedCountDownLatch("refetch", 2);
    final AtomicReference<Response<ReviewsByEpisodeQuery.Data>> empireReviewsWatchResponse = new AtomicReference<>();
    ApolloQueryWatcher<ReviewsByEpisodeQuery.Data> queryWatcher = apolloClient.query(new ReviewsByEpisodeQuery(Episode.EMPIRE))
        .watcher()
        .refetchResponseFetcher(NETWORK_FIRST)
        .enqueueAndWatch(new ApolloCall.Callback<ReviewsByEpisodeQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<ReviewsByEpisodeQuery.Data> response) {
            empireReviewsWatchResponse.set(response);
            countDownBeforeMutationLatch.countDown();
            countDownRefetchLatch.countDown();
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
          }
        });

    countDownBeforeMutationLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    CreateReviewMutation mutation = new CreateReviewMutation(
        Episode.EMPIRE,
        ReviewInput.builder().stars(5).commentary("Awesome").favoriteColor(ColorInput.builder().build()).build()
    );
    Rx2Apollo
        .from(apolloClient.mutate(mutation).refetchQueries(queryWatcher.operation().name()))
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    countDownRefetchLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    assertThat(server.getRequestCount()).isEqualTo(3);

    Response<ReviewsByEpisodeQuery.Data> empireReviewsQueryResponse = empireReviewsWatchResponse.get();
    assertThat(empireReviewsQueryResponse.data().reviews()).hasSize(4);
    assertThat(empireReviewsQueryResponse.data().reviews().get(3).stars()).isEqualTo(5);
    assertThat(empireReviewsQueryResponse.data().reviews().get(3).commentary()).isEqualTo("Awesome");

    queryWatcher.cancel();
  }
}
