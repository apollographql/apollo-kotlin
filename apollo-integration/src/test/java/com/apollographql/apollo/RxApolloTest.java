package com.apollographql.apollo;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.rx.RxApollo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

import static com.apollographql.apollo.Utils.mockResponse;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.apollographql.apollo.integration.normalizer.type.Episode.EMPIRE;
import static com.apollographql.apollo.integration.normalizer.type.Episode.NEWHOPE;
import static com.google.common.truth.Truth.assertThat;

public class RxApolloTest {
  private ApolloClient apolloClient;
  @Rule public final MockWebServer server = new MockWebServer();

  private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
  private static final String FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json";

  @Before public void setup() {
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .dispatcher(new Dispatcher(Utils.immediateExecutorService()))
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .dispatcher(Utils.immediateExecutor())
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .build();
  }

  @Test
  public void callProducesValue() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .subscribe(testSubscriber);

    testSubscriber.assertNoErrors();
    testSubscriber.assertCompleted();
    testSubscriber.assertValueCount(1);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");
  }

  @Test
  public void callIsCanceledWhenUnsubscribe() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    TestScheduler scheduler = new TestScheduler();
    Subscription subscription = RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .observeOn(scheduler)
        .subscribe(testSubscriber);

    subscription.unsubscribe();
    scheduler.triggerActions();
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

    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(0);
    testSubscriber.assertCompleted();
  }

  @Test
  public void prefetchIsCanceledWhenDisposed() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    final TestSubscriber<Void> testSubscriber = new TestSubscriber<>();
    TestScheduler scheduler = new TestScheduler();
    Subscription subscription = RxApollo
        .from(apolloClient.prefetch(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .observeOn(scheduler)
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
    scheduler.triggerActions();
    testSubscriber.assertNotCompleted();
    testSubscriber.assertValueCount(0);
    testSubscriber.assertNoErrors();
    assertThat(subscription.isUnsubscribed()).isTrue();
  }

  @Test
  public void queryWatcherUpdatedSameQueryDifferentResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));

    final TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .subscribe(testSubscriber);
    apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE)))
        .responseFetcher(NETWORK_ONLY)
        .enqueue(null);
    testSubscriber.assertValueCount(2);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");
    assertThat(testSubscriber.getOnNextEvents().get(1).data().hero().name()).isEqualTo("Artoo");
  }

  @Test
  public void queryWatcherNotUpdatedSameQuerySameResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    final TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .subscribe(testSubscriber);
    apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY);

    testSubscriber.assertValueCount(1);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");
  }

  @Test
  public void queryWatcherUpdatedDifferentQueryDifferentResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"));

    final TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .subscribe(testSubscriber);
    apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(NEWHOPE))).enqueue(null);

    testSubscriber.assertValueCount(2);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");
    assertThat(testSubscriber.getOnNextEvents().get(1).data().hero().name()).isEqualTo("Artoo");
  }

  @Test
  public void queryWatcherNotCalledWhenCanceled() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));

    final TestSubscriber<Response<EpisodeHeroNameQuery.Data>> testSubscriber = new TestSubscriber<>();
    TestScheduler scheduler = new TestScheduler();
    Subscription subscription = RxApollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .observeOn(scheduler)
        .subscribe(testSubscriber);

    scheduler.triggerActions();
    apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY).enqueue(null);
    subscription.unsubscribe();
    scheduler.triggerActions();
    testSubscriber.assertValueCount(1);
    assertThat(testSubscriber.getOnNextEvents().get(0).data().hero().name()).isEqualTo("R2-D2");
  }
}
