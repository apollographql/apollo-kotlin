package com.apollographql.apollo;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.rx.RxApollo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

import static com.apollographql.apollo.Utils.TIME_OUT_SECONDS;
import static com.apollographql.apollo.Utils.mockResponse;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.apollographql.apollo.integration.normalizer.type.Episode.EMPIRE;
import static com.apollographql.apollo.integration.normalizer.type.Episode.NEWHOPE;
import static com.google.common.truth.Truth.assertThat;

public class RxApolloTest {
  private ApolloClient apolloClient;
  private MockWebServer server;

  private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
  private static final String FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json";

  @Before public void setup() {
    server = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .build();
  }

  @After
  public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignore) {
      //ignore
    }
  }

  @Test
  public void callProducesValue() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .subscribe(testSubscriber);

    testSubscriber.awaitTerminalEvent(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testSubscriber.assertNoErrors();
    testSubscriber.assertCompleted();
    testSubscriber.assertValueCount(1);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");

  }

  @Test
  public void callIsCanceledWhenUnsubscribe() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID).setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));

    TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    Subscription subscription = RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .subscribe(testSubscriber);

    subscription.unsubscribe();
    testSubscriber.awaitTerminalEvent(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testSubscriber.assertNotCompleted();
    testSubscriber.assertValueCount(0);
    testSubscriber.assertNoErrors();
    assertThat(testSubscriber.isUnsubscribed()).isTrue();
  }

  @Test
  public void prefetchCompletes() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    RxApollo
        .from(apolloClient.prefetch(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .subscribe(testSubscriber);

    testSubscriber.awaitTerminalEvent(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(0);
    testSubscriber.assertCompleted();
  }

  @Test
  public void prefetchIsCanceledWhenDisposed() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID).setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));

    final TestSubscriber<Void> testSubscriber = new TestSubscriber<>();
    Subscription subscription = RxApollo
        .from(apolloClient.prefetch(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .subscribe(new Action0() {
          @Override public void call() {
            testSubscriber.onCompleted();
          }
        }, new Action1<Throwable>() {
          @Override public void call(Throwable throwable) {
            testSubscriber.onError(throwable);
          }
        });
    subscription.unsubscribe();
    testSubscriber.awaitTerminalEvent(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testSubscriber.assertNotCompleted();
    testSubscriber.assertValueCount(0);
    testSubscriber.assertNoErrors();
    assertThat(subscription.isUnsubscribed()).isTrue();
  }

  @Test
  public void queryWatcherUpdatedSameQueryDifferentResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    final TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .doOnNext(new Action1<Response<EpisodeHeroNameQuery.Data>>() {
          AtomicBoolean executed = new AtomicBoolean();

          @Override public void call(Response<EpisodeHeroNameQuery.Data> response) {
            if (executed.compareAndSet(false, true)) {
              try {
                server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
              } catch (Exception ignore) {
              }

              apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE)))
                  .responseFetcher(NETWORK_ONLY)
                  .enqueue(null);
            }
          }
        })
        .take(2)
        .subscribe(testSubscriber);

    testSubscriber.awaitTerminalEvent(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testSubscriber.assertValueCount(2);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");
    assertThat(testSubscriber.getOnNextEvents().get(1).data().hero().name()).isEqualTo("Artoo");
  }

  @Test
  public void queryWatcherNotUpdatedSameQuerySameResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    final TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .doOnNext(new Action1<Response<EpisodeHeroNameQuery.Data>>() {
          AtomicBoolean executed = new AtomicBoolean();

          @Override public void call(Response<EpisodeHeroNameQuery.Data> response) {
            if (executed.compareAndSet(false, true)) {
              try {
                server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
              } catch (Exception ignore) {
              }
              apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY)
                  .enqueue(null);
            }
          }
        })
        .take(2)
        .subscribe(testSubscriber);

    testSubscriber.awaitTerminalEvent(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testSubscriber.assertValueCount(1);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");
  }

  @Test
  public void queryWatcherUpdatedDifferentQueryDifferentResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    final TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .doOnNext(new Action1<Response<EpisodeHeroNameQuery.Data>>() {
          AtomicBoolean executed = new AtomicBoolean();

          @Override public void call(Response<EpisodeHeroNameQuery.Data> response) {
            if (executed.compareAndSet(false, true)) {
              try {
                server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"));
              } catch (Exception ignore) {
              }
              apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(NEWHOPE))).enqueue(null);
            }
          }
        })
        .take(2)
        .subscribe(testSubscriber);

    testSubscriber.awaitTerminalEvent(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testSubscriber.assertValueCount(2);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");
    assertThat(testSubscriber.getOnNextEvents().get(1).data().hero().name()).isEqualTo("Artoo");
  }

  @Test
  public void queryWatcherNotCalledWhenCanceled() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    final TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    Subscription subscription = RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .doOnNext(new Action1<Response<EpisodeHeroNameQuery.Data>>() {
          AtomicBoolean executed = new AtomicBoolean();

          @Override public void call(Response<EpisodeHeroNameQuery.Data> response) {
            if (executed.compareAndSet(false, true)) {
              try {
                server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE).setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));
              } catch (Exception ignore) {
              }
              apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY).enqueue(null);
            }
          }
        })
        .subscribe(testSubscriber);

    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(TIME_OUT_SECONDS));
    } catch (Exception ignore) {
    }

    subscription.unsubscribe();

    testSubscriber.awaitTerminalEvent(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testSubscriber.assertValueCount(1);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");
  }
}
