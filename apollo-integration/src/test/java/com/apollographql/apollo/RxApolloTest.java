package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.rx.RxApollo;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import rx.Observer;
import rx.Subscription;
import rx.observers.AssertableSubscriber;
import rx.observers.TestSubscriber;

import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.google.common.truth.Truth.assertThat;

public class RxApolloTest {

  private ApolloClient apolloClient;
  private MockWebServer server;

  private static final int RX_DELAY_SECONDS = 2;
  private static final long TIME_OUT_SECONDS = 3;

  @Before public void setUp() {
    server = new MockWebServer();
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

  @Test public void testRxCallProducesValue() throws IOException {
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    Response<EpisodeHeroNameQuery.Data> response = RxApollo
        .from(apolloClient.query(query))
        .test()
        .awaitTerminalEvent()
        .assertNoErrors()
        .assertCompleted()
        .getOnNextEvents()
        .get(0);
    assertThat(response.data().hero().name()).isEqualTo("R2-D2");
  }

  @Test public void textRxCallIsCancelledWhenUnsubscribed() throws IOException {
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloCall<EpisodeHeroNameQuery.Data> call = apolloClient.query(query);
    TestSubscriber<Response<EpisodeHeroNameQuery.Data>> subscriber = new TestSubscriber<>();
    Subscription subscription = RxApollo
        .from(call)
        .delay(RX_DELAY_SECONDS, TimeUnit.SECONDS)
        .subscribe(subscriber);
    subscription.unsubscribe();
    subscriber
        .assertUnsubscribed();
    subscriber.assertNoValues();

  }

  @Test public void testRxPrefetchCompletes() throws IOException {
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    RxApollo
        .from(apolloClient.prefetch(query))
        .test()
        .awaitTerminalEvent()
        .assertNoErrors()
        .assertCompleted();
  }

  @Test public void testRxPrefetchIsCanceledWhenUnsubscribed() throws IOException {

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloPrefetch prefetch = apolloClient.prefetch(query);

    AssertableSubscriber<Void> subscriber = RxApollo
        .from(prefetch)
        .delay(RX_DELAY_SECONDS, TimeUnit.SECONDS)
        .test();

    subscriber.unsubscribe();

    subscriber.assertUnsubscribed();
    subscriber.assertNotCompleted();
  }

  @Test
  public void testRxQueryWatcherUpdated_SameQuery_DifferentResults() throws IOException, InterruptedException,
      TimeoutException, ApolloException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();

    RxApollo.from(watcher).subscribe(new Observer<Response<EpisodeHeroNameQuery.Data>>() {
      @Override public void onCompleted() {
      }

      @Override public void onError(Throwable e) {
        Assert.fail(e.getMessage());
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }

      @Override public void onNext(Response<EpisodeHeroNameQuery.Data> response) {
        if (secondResponseLatch.getCount() == 2) {
          assertThat(response.data().hero().name()).isEqualTo("R2-D2");
        } else if (secondResponseLatch.getCount() == 1) {
          assertThat(response.data().hero().name()).isEqualTo("Artoo");
        }
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }
    });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    //Another newer call gets updated information
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));
    apolloClient.query(query).responseFetcher(NETWORK_ONLY).execute();
    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void testRxQueryWatcherNotUpdated_SameQuery_SameResults() throws IOException, InterruptedException, TimeoutException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();

    RxApollo.from(watcher).subscribe(new Observer<Response<EpisodeHeroNameQuery.Data>>() {
      @Override public void onCompleted() {
      }

      @Override public void onError(Throwable e) {
        Assert.fail(e.getMessage());
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }

      @Override public void onNext(Response<EpisodeHeroNameQuery.Data> response) {
        assertThat(response.data().hero().name()).isEqualTo("R2-D2");
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
        if (secondResponseLatch.getCount() == 0) {
          Assert.fail("Received two callbacks, although data should not have changed.");
        }
      }
    });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    apolloClient.query(query).responseFetcher(NETWORK_ONLY).enqueue(null);

    // Wait 3 seconds to make sure no double callback.
    // Successful if timeout _is_ reached
    secondResponseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void testQueryRxWatcherUpdated_DifferentQuery_DifferentResults() throws IOException, InterruptedException,
      TimeoutException, ApolloException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();

    RxApollo.from(watcher).subscribe(new Observer<Response<EpisodeHeroNameQuery.Data>>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {
        Assert.fail(e.getMessage());
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }

      @Override public void onNext(Response<EpisodeHeroNameQuery.Data> response) {
        if (secondResponseLatch.getCount() == 2) {
          assertThat(response.data().hero().name()).isEqualTo("R2-D2");
        } else if (secondResponseLatch.getCount() == 1) {
          assertThat(response.data().hero().name()).isEqualTo("Artoo");
        }
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }
    });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    HeroAndFriendsNamesWithIDsQuery friendsQuery = HeroAndFriendsNamesWithIDsQuery.builder()
        .episode(Episode.NEWHOPE)
        .build();

    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"));
    apolloClient.query(friendsQuery).responseFetcher(NETWORK_ONLY).execute();
    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
